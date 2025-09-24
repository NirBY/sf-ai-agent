# Salesforce AI Agent (Java + Ollama + Qdrant) — Synology Docker

An on-prem, privacy-first AI agent that monitors a Salesforce Case list view, and for each new case it generates:

- Hebrew summary (full Hebrew support)
- תשובת טיוטה (Hebrew draft reply)
- RAG over your local knowledge base
- Memory to avoid re-processing already handled cases (SQLite)

Runs fully local on Synology using Docker Compose, Ollama (LLM + embeddings), and Qdrant (vector DB).

## ✨ Features

- Watches Salesforce list view (configurable)
- Produces סיכום קצר + טיוטת תשובה בעברית, עם שאלות הבהרה אם צריך
- RAG: retrieves top-K snippets from mounted docs and injects them into the prompt
- Posts output as a Case Comment
- Remembers handled Case IDs in SQLite to prevent duplicates
- **NEW**: Interactive RAG API for document ingestion and Hebrew Q&A
  - Upload documents (Excel, Word, PDF, text files)
  - Ingest web pages via URL
  - Ask questions in Hebrew and get answers from your knowledge base

## 🏗 Architecture

### High-level view
```
                        ┌──────────────────────────────┐
                        │          Salesforce          │
                        │  • Case ListView / SOQL      │
                        │  • Case / CaseComment        │
                        └──────────────┬───────────────┘
                                       │ REST (OAuth)
                                       │
                 ┌─────────────────────▼─────────────────────┐
                 │           sf-ai-agent (Spring Boot)       │
                 │  - Scheduler / CaseWatcher                │
                 │  - SalesforceAuthService + Client         │
                 │  - ListViewService / CaseService          │
                 │  - RAG API (upload/url/query)             │
                 │  - RagService (chunk→embed→Qdrant)        │
                 │  - LlmProvider (Ollama/OpenAI)            │
                 │  - TextExtractor (Tika + Jsoup)           │
                 │  - Metrics (Actuator + Micrometer)        │
                 │  - Memory (SQLite handled_cases)          │
                 └───────────┬───────────────┬──────────────┘
                             │               │
                             │               │ vectors (HTTP)
                             │               ▼
                             │        ┌───────────────┐
                             │        │   Qdrant      │
  Ingest (files/urls)        │        │  Vector DB    │
  ┌───────────────────────┐  │        └───────────────┘
  │  /rag/ingest/upload   │──┘
  │  /rag/ingest/url      │─────┐
  │  /rag/reindex         │     │
  └───────────────────────┘     │
                                │
                                │ chat/embeddings (HTTP)
                        ┌───────▼────────────────┐
                        │  LLM Runtime           │
                        │  (choose one)          │
                        │  • Ollama (local)      │
                        │    http://ollama:11434 │
                        │  • Ollama (external)   │
                        │    http://X.Y.Z.W:11434│
                        │  • OpenAI (optional)   │
                        │    https://api.openai… │
                        └────────────────────────┘
```

### Inside the agent (modules)

**Scheduler / CaseWatcher**
- Polls list view by label (SF_CASE_LISTVIEW_LABEL) or falls back to SOQL "All Open Cases" when empty
- For each new Case: fetch → RAG retrieve → LLM → post CaseComment → mark in SQLite

**Salesforce adapters**
- SalesforceAuthService (OAuth Username+Password), SalesforceClient (401 retry), ListViewService, CaseService

**RAG**
- TextExtractorService (Apache Tika + Jsoup) → normalizes DOC/DOCX/XLS/XLSX/PDF/HTML/TXT and URLs
- RagService: chunk (≈1000 chars, overlap 200) → embed (Ollama or OpenAI) → Qdrant upsert/search → build Hebrew context → answer
- RagController:
  - POST /rag/ingest/upload (multipart file)
  - POST /rag/ingest/url (fetch + parse)
  - POST /rag/reindex (rescan mounted KB folder)
  - POST /rag/query (hebrew question → answer + sources)

**LLM Provider**
- Pluggable via env: LLM_PROVIDER=ollama (default) or openai
- Models set via .env (chat + embedding)

**Metrics / Observability**
- Spring Actuator /actuator/health, /actuator/metrics, /actuator/prometheus
- Custom metrics: processed/skipped/errors + timers for SF/RAG/LLM

**Memory**
- CaseMemoryRepository (SQLite) tracks handled Case IDs to avoid duplicates


### Core flows (sequence)

**1) Case assist (automatic comment on new Case)**
- Poll (ListView or SOQL) → newest Cases
- Skip if CaseId in SQLite
- GET Case (Subject/Description)
- RAG retrieve from Qdrant (top-K)
- LLM chat (Hebrew prompt + context) → summary + תשובת טיוטה + שאלות חסר
- POST CaseComment back to Salesforce
- Mark handled in SQLite

**2) RAG ingest**
- POST /rag/ingest/upload → save to /data/knowledge → Tika → chunk → embed → Qdrant upsert
- POST /rag/ingest/url → fetch HTML → text → chunk → embed → Qdrant upsert
- POST /rag/reindex → walk /data/knowledge and rebuild vectors

**3) RAG Q&A (Hebrew)**
- POST /rag/query with { question, topK, maxTokens }
- Embed question → Qdrant search → build Hebrew context with sources
- LLM chat → return { answer, sources[] }

### Deployment layouts

**A) Local Ollama (default stack)**
- docker-compose.yml runs: ollama, qdrant, sf-agent on the Synology
- Agent uses OLLAMA_BASE=http://ollama:11434
- Good for data residency (no prompts leave NAS)

**B) External Ollama**
- Use docker-compose.external-ollama.yml: only qdrant + sf-agent on Synology
- OLLAMA_BASE=http://<external-host>:11434
- Secure with LAN/TLS/reverse proxy; prompts/embeddings traverse the network to that host

**C) OpenAI (optional)**
- Set LLM_PROVIDER=openai and provide OPENAI_* in .env
- Agent still stores vectors in Qdrant on Synology

### Ports & endpoints

- **sf-agent**: 8080
  - GET /actuator/health, /actuator/metrics, /actuator/prometheus
  - POST /rag/ingest/upload (multipart), /rag/ingest/url, /rag/reindex, /rag/query
- **qdrant**: 6333 (HTTP), 6334 (gRPC)
- **ollama**: 11434 (HTTP)

### Configuration knobs (env)

- **Provider**: LLM_PROVIDER=ollama|openai
- **Ollama**: OLLAMA_BASE, OLLAMA_CHAT_MODEL, OLLAMA_EMBED_MODEL
- **OpenAI**: OPENAI_BASE, OPENAI_API_KEY, OPENAI_CHAT_MODEL, OPENAI_EMBED_MODEL
- **Salesforce**: SF_LOGIN_URL, SF_CLIENT_ID, SF_CLIENT_SECRET, SF_USERNAME, SF_PASSWORD, SF_API_VERSION, SF_CASE_LISTVIEW_LABEL (empty → SOQL "All Open Cases")
- **RAG**: KB_PATH (mounted /data/knowledge), QDRANT_URL, QDRANT_COLLECTION
- **Observability**: METRICS_ENABLED=true, PROMETHEUS_SCRAPE_PATH=/actuator/prometheus
- **General**: POLL_SECONDS, TZ, MEMORY_DB

### Security & data flow notes

**Data leaving the NAS**
- **Local Ollama**: none (LLM calls are local)
- **External Ollama / OpenAI**: the question/context (case text + RAG snippets) are sent to that endpoint over HTTPS/HTTP—secure accordingly (TLS, IP allowlists, reverse-proxy auth)

**Stored data**
- **Qdrant**: vectors + payloads (text chunks + source metadata)
- **SQLite**: only Case IDs + timestamps
- **Least-privilege Salesforce user**: read Case, create CaseComment

## 📁 Repository Layout

sf-ai-agent/
├─ docker/
│  ├─ docker-compose.yml
│  ├─ docker-compose.external-ollama.yml
│  └─ monitoring/
│     └─ prometheus.yml
├─ app/
│  ├─ pom.xml
│  └─ src/main/java/com/nby/agent/
│     ├─ AgentApplication.java
│     ├─ config/
│     │  ├─ AppConfig.java
│     │  └─ PromptTemplates.java
│     ├─ salesforce/
│     │  ├─ SalesforceAuthService.java
│     │  ├─ SalesforceClient.java
│     │  ├─ ListViewService.java
│     │  └─ CaseService.java
│     ├─ llm/
│     │  ├─ LlmFactory.java
│     │  ├─ LlmProvider.java
│     │  ├─ OllamaClient.java
│     │  ├─ OpenAIClient.java
│     │  └─ RagService.java
│     ├─ rag/
│     │  ├─ RagController.java
│     │  ├─ DocumentIngestService.java
│     │  └─ TextExtractorService.java
│     ├─ metrics/
│     │  └─ MetricsService.java
│     ├─ storage/
│     │  ├─ CaseMemoryRepository.java
│     │  └─ CaseMemoryEntity.java
│     └─ scheduler/
│        └─ CaseWatcher.java
├─ app/src/main/resources/application.yml
├─ app/Dockerfile
└─ README.md

## ✅ Requirements

- Synology NAS with Container Manager (Docker) enabled
- Java 21 + Maven (to build the JAR once)
  - Alternative: use the optional multi-stage Dockerfile snippet below to build inside Docker
- Salesforce org with API access:
  - Connected App (Client ID/Secret)
  - Service user with API Enabled, Read Case, Create CaseComment
- Open LAN ports: 11434 (Ollama), 6333 (Qdrant); agent itself runs internally

## 🔐 Salesforce Setup (one-time)

### Connected App (Setup → App Manager → New Connected App)
- Enable OAuth
- Add any callback URL (not used in username-password flow)
- Save the Consumer Key (Client ID) and Consumer Secret

### Service User
- Has API access; permission to read Case and create CaseComment
- Obtain the Security Token

### List View
- Ensure a Case list view with your desired label (or leave empty for default: "All Open Cases" via SOQL)
- Include Id column; sort newest first (e.g., CreatedDate DESC)

## 🔐 Salesforce Credentials — How to Obtain Each Value

### Quick mapping
| ENV var | What it is | Where to get it |
|---------|------------|-----------------|
| SF_LOGIN_URL | OAuth authorization server | Production: https://login.salesforce.com • Sandbox: https://test.salesforce.com • My Domain/SSO: https://<MYDOMAIN>.my.salesforce.com |
| SF_CLIENT_ID | Connected App Consumer Key | Setup → App Manager → your Connected App → ▼ → View → Consumer Key |
| SF_CLIENT_SECRET | Connected App Consumer Secret | Same screen → Consumer Secret (click Reveal) |
| SF_USERNAME | Username of the integration user | Setup → Users → pick the integration user → copy Username |
| SF_PASSWORD | Password + SecurityToken concatenated | Integration user's password, appended with their Security Token (see below) |
| SF_API_VERSION | REST API version | e.g., v60.0 or newer supported by your org |

### A) Create (or reuse) a Connected App

In Salesforce (Setup gear → Setup).

1. Search App Manager → New Connected App.
2. Fill "Basic Information" (name, email).
3. Check Enable OAuth Settings.
4. Callback URL: any valid URL is fine for the password grant (e.g., https://localhost/done).
5. Selected OAuth Scopes: add Access and manage your data (api) (and optionally refresh_token scope).
6. Save. In App Manager, click your app's ▼ → View:
   - Consumer Key → SF_CLIENT_ID
   - Consumer Secret → SF_CLIENT_SECRET (click Reveal)
7. (Recommended) Manage → Edit Policies:
   - Permitted Users: Admin approved users are pre-authorized
   - IP Relaxation: Relax IP restrictions
8. Assign your integration user via Profiles/Permission Sets

### B) Integration User

1. Setup → Users → create/choose a dedicated user (e.g., "Integration User").
2. Profile/Perms: API Enabled, Read Case, Create CaseComment (and anything else you need).
3. Copy their Username → SF_USERNAME.

### C) Security Token (append to password)

1. Log in as that integration user, avatar → Settings → Reset My Security Token.
2. Salesforce emails the token to that user.
3. Compose SF_PASSWORD like: `<UserPassword><SecurityToken>`

**Example:** password Pa$$w0rd! and token ABCD1234XYZ → Pa$$w0rd!ABCD1234XYZ

If your org trusts your IP range, a token may not be required. Using password+token works universally.

### D) Picking the login URL

- Production org: https://login.salesforce.com
- Sandbox org: https://test.salesforce.com
- My Domain/SSO enforced: https://<MYDOMAIN>.my.salesforce.com

The token response provides instance_url automatically; the agent uses it afterward.

### E) Choosing the API version (SF_API_VERSION)

- Safe/current example: v60.0
- To discover latest supported: After obtaining a token, GET https://<instance_url>/services/data/ → pick the highest (e.g., v6x.0), then set SF_API_VERSION=v6x.0.

### F) Optional verification (PowerShell)

**Token:**
```powershell
$LoginUrl = "https://login.salesforce.com"         # or test/my domain
$ClientId = "<SF_CLIENT_ID>"
$ClientSecret = "<SF_CLIENT_SECRET>"
$Username = "<SF_USERNAME>"
$Password = "<SF_PASSWORD>"  # password + token

$Body = "grant_type=password&client_id=$ClientId&client_secret=$ClientSecret&username=$Username&password=$Password"
Invoke-RestMethod -Method Post -Uri "$LoginUrl/services/oauth2/token" -ContentType "application/x-www-form-urlencoded" -Body $Body
```

**API versions:**
```powershell
$Token = "<access_token>"
$Instance = "<instance_url>"
Invoke-RestMethod -Headers @{ Authorization = "Bearer $Token" } -Uri "$Instance/services/data/"
```

### Common pitfalls

- **invalid_grant:** wrong password/token, user not allowed on Connected App, or IP restrictions.
- **invalid_client:** Consumer Key/Secret mismatch—copy again from App Manager → View.
- **SSO/My Domain required** → use your https://<MYDOMAIN>.my.salesforce.com as SF_LOGIN_URL.

### TL;DR template

```bash
SF_LOGIN_URL=https://login.salesforce.com           # or https://test.salesforce.com or https://<MYDOMAIN>.my.salesforce.com
SF_CLIENT_ID=3MVG9...<Consumer Key>
SF_CLIENT_SECRET=195...<Consumer Secret>
SF_USERNAME=svc.integration@yourorg.com
SF_PASSWORD=<UserPassword><SecurityToken>           # concatenated
SF_API_VERSION=v60.0                                # or your org's latest
```

## ⚙️ Environment File

```bash
# Salesforce
SF_LOGIN_URL=https://login.salesforce.com
SF_CLIENT_ID=YOUR_CLIENT_ID
SF_CLIENT_SECRET=YOUR_CLIENT_SECRET
SF_USERNAME=svc.example@yourorg.com
SF_PASSWORD=YourPasswordConcatenatedWithSecurityToken
SF_API_VERSION=v60.0

# List view label (leave empty to use default: "All Open Cases" via SOQL)
SF_CASE_LISTVIEW_LABEL=YOUR_LISTVIEW_LABEL

# LLM Provider (ollama or openai)
LLM_PROVIDER=ollama

# Ollama Configuration
OLLAMA_BASE=http://ollama:11434
OLLAMA_CHAT_MODEL=llama3.1:8b
OLLAMA_EMBED_MODEL=mxbai-embed-large

# OpenAI Configuration (optional)
OPENAI_BASE=https://api.openai.com/v1
OPENAI_API_KEY=your_openai_api_key
OPENAI_CHAT_MODEL=gpt-4
OPENAI_EMBED_MODEL=text-embedding-3-large

# RAG Configuration
KB_PATH=/data/knowledge
QDRANT_URL=http://qdrant:6333
QDRANT_COLLECTION=sf_kb

# Observability
METRICS_ENABLED=true
PROMETHEUS_SCRAPE_PATH=/actuator/prometheus

# General
POLL_SECONDS=60
TZ=Asia/Jerusalem
MEMORY_DB=/data/handled_cases.db
```

## 📊 Prometheus Metrics & Observability

The application exposes comprehensive metrics via Prometheus for monitoring and observability:

### Available Metrics

#### Counters
- `sfagent_cases_processed` - Total number of cases processed
- `sfagent_cases_skipped_handled` - Cases skipped because already handled
- `sfagent_case_comments_posted` - Case comments posted to Salesforce
- `sfagent_errors_salesforce` - Salesforce API errors
- `sfagent_errors_llm` - LLM (Ollama) errors
- `sfagent_errors_rag` - RAG/vector database errors

#### Timers (Duration Metrics)
- `sfagent_sf_fetch_case_seconds` - Time to fetch individual cases from Salesforce
- `sfagent_sf_list_seconds` - Time to fetch list view or SOQL queries
- `sfagent_sf_post_case_comment_seconds` - Time to post case comments
- `sfagent_sf_auth_seconds` - Salesforce authentication duration
- `sfagent_qdrant_get_seconds` - Qdrant GET request duration
- `sfagent_qdrant_post_seconds` - Qdrant POST request duration
- `sfagent_qdrant_put_seconds` - Qdrant PUT request duration
- `sfagent_db_query_seconds` - SQLite database query duration
- `sfagent_db_insert_seconds` - SQLite database insert duration
- `sfagent_rag_retrieve_seconds` - RAG retrieval from vector DB duration
- `sfagent_rag_ingest_seconds` - RAG document ingestion duration
- `sfagent_llm_chat_seconds` - LLM chat completion duration
- `sfagent_llm_embed_seconds` - LLM embedding generation duration

### Accessing Metrics

Metrics are exposed at: `http://localhost:8080/actuator/prometheus`

### Prometheus Configuration

Add to your `prometheus.yml`:

```yaml
scrape_configs:
  - job_name: 'sf-ai-agent'
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['sf-ai-agent:8080']
```

### Sample Queries

```promql
# Cases processed per minute
rate(sfagent_cases_processed[1m]) * 60

# Average case processing time
rate(sfagent_sf_fetch_case_seconds_sum[5m]) / rate(sfagent_sf_fetch_case_seconds_count[5m])

# Error rate by type
rate(sfagent_errors_salesforce[5m])
rate(sfagent_errors_llm[5m])
rate(sfagent_errors_rag[5m])

# LLM response time
histogram_quantile(0.95, rate(sfagent_llm_chat_seconds_bucket[5m]))
```

## 🤖 Interactive RAG API

The application now includes a comprehensive RAG (Retrieval Augmented Generation) API for document ingestion and Hebrew Q&A:

### Endpoints

#### Upload Documents
```bash
POST /rag/ingest/upload
Content-Type: multipart/form-data

# Upload Excel, Word, PDF, or text files
curl -X POST http://localhost:8080/rag/ingest/upload \
  -F "file=@document.pdf"
```

#### Ingest Web Pages
```bash
POST /rag/ingest/url
Content-Type: application/x-www-form-urlencoded

# Ingest content from any website
curl -X POST http://localhost:8080/rag/ingest/url \
  -d "url=https://example.com/documentation"
```

#### Ask Questions in Hebrew
```bash
POST /rag/query
Content-Type: application/json

# Ask questions and get Hebrew answers from your knowledge base
curl -X POST http://localhost:8080/rag/query \
  -H "Content-Type: application/json" \
  -d '{
    "question": "מה התהליך לטיפול בבעיות לקוחות?",
    "topK": 5,
    "maxTokens": 800
  }'
```

#### Reindex Knowledge Base
```bash
POST /rag/reindex

# Reindex all documents in the knowledge base
curl -X POST http://localhost:8080/rag/reindex
```

### Response Format

```json
{
  "answer": "תשובה מפורטת בעברית...",
  "sources": [
    {
      "name": "document.pdf",
      "uri": null,
      "snippet": "קטע רלוונטי מהמסמך...",
      "score": 0.95
    }
  ]
}
```

### Supported File Types

- **Documents**: PDF, Word (.docx), Excel (.xlsx), PowerPoint (.pptx)
- **Text**: .txt, .md, .csv
- **Web**: Any HTML page via URL
- **Archives**: .zip (extracted automatically)

### Hebrew Language Support

- Questions can be asked in Hebrew
- Answers are generated in Hebrew
- Context-aware responses based on your documents
- Source attribution for transparency

## 🧱 Docker Compose

### A) Local Ollama (default)

```yaml
version: "3.9"

services:
  ollama:
    image: ollama/ollama:latest
    container_name: ollama
    restart: unless-stopped
    environment:
      - OLLAMA_KEEP_ALIVE=24h
    volumes:
      - /volume2/ollama:/root/.ollama
    ports:
      - "11434:11434"

  qdrant:
    image: qdrant/qdrant:latest
    container_name: qdrant
    restart: unless-stopped
    volumes:
      - /volume2/qdrant:/qdrant/storage
    ports:
      - "6333:6333"
      - "6334:6334"

  sf-agent:
    build:
      context: ../app
      dockerfile: Dockerfile
    container_name: sf-ai-agent
    restart: unless-stopped
    depends_on:
      - ollama
      - qdrant
    environment:
      - SF_LOGIN_URL=${SF_LOGIN_URL}
      - SF_CLIENT_ID=${SF_CLIENT_ID}
      - SF_CLIENT_SECRET=${SF_CLIENT_SECRET}
      - SF_USERNAME=${SF_USERNAME}
      - SF_PASSWORD=${SF_PASSWORD}
      - SF_API_VERSION=${SF_API_VERSION}
      - SF_CASE_LISTVIEW_LABEL=${SF_CASE_LISTVIEW_LABEL}

      - LLM_PROVIDER=ollama
      - OLLAMA_BASE=http://ollama:11434
      - OLLAMA_CHAT_MODEL=${OLLAMA_CHAT_MODEL}
      - OLLAMA_EMBED_MODEL=${OLLAMA_EMBED_MODEL}

      - QDRANT_URL=http://qdrant:6333
      - QDRANT_COLLECTION=sf_kb
      - KB_PATH=/data/knowledge

      - POLL_SECONDS=${POLL_SECONDS}
      - TZ=${TZ}
    volumes:
      - /volume2/knowledge:/data/knowledge
      - /volume2/handled_cases:/data
    ports:
      - "8080:8080"
```

### B) External Ollama

```yaml
version: "3.9"

services:
  qdrant:
    image: qdrant/qdrant:latest
    container_name: qdrant
    restart: unless-stopped
    volumes:
      - /volume2/qdrant:/qdrant/storage
    ports:
      - "6333:6333"
      - "6334:6334"

  sf-agent:
    build:
      context: ../app
      dockerfile: Dockerfile
    container_name: sf-ai-agent
    restart: unless-stopped
    depends_on:
      - qdrant
    environment:
      - SF_LOGIN_URL=${SF_LOGIN_URL}
      - SF_CLIENT_ID=${SF_CLIENT_ID}
      - SF_CLIENT_SECRET=${SF_CLIENT_SECRET}
      - SF_USERNAME=${SF_USERNAME}
      - SF_PASSWORD=${SF_PASSWORD}
      - SF_API_VERSION=${SF_API_VERSION}
      - SF_CASE_LISTVIEW_LABEL=${SF_CASE_LISTVIEW_LABEL}

      - LLM_PROVIDER=ollama
      - OLLAMA_BASE=${OLLAMA_BASE}
      - OLLAMA_CHAT_MODEL=${OLLAMA_CHAT_MODEL}
      - OLLAMA_EMBED_MODEL=${OLLAMA_EMBED_MODEL}

      - QDRANT_URL=http://qdrant:6333
      - QDRANT_COLLECTION=sf_kb
      - KB_PATH=/data/knowledge

      - POLL_SECONDS=${POLL_SECONDS}
      - TZ=${TZ}
    volumes:
      - /volume2/knowledge:/data/knowledge
      - /volume2/handled_cases:/data
    ports:
      - "8080:8080"
```

### C) OpenAI (optional)

```yaml
version: "3.9"

services:
  qdrant:
    image: qdrant/qdrant:latest
    container_name: qdrant
    restart: unless-stopped
    volumes:
      - /volume2/qdrant:/qdrant/storage
    ports:
      - "6333:6333"
      - "6334:6334"

  sf-agent:
    build:
      context: ../app
      dockerfile: Dockerfile
    container_name: sf-ai-agent
    restart: unless-stopped
    depends_on:
      - qdrant
    environment:
      - SF_LOGIN_URL=${SF_LOGIN_URL}
      - SF_CLIENT_ID=${SF_CLIENT_ID}
      - SF_CLIENT_SECRET=${SF_CLIENT_SECRET}
      - SF_USERNAME=${SF_USERNAME}
      - SF_PASSWORD=${SF_PASSWORD}
      - SF_API_VERSION=${SF_API_VERSION}
      - SF_CASE_LISTVIEW_LABEL=${SF_CASE_LISTVIEW_LABEL}

      - LLM_PROVIDER=openai
      - OPENAI_BASE=${OPENAI_BASE}
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - OPENAI_CHAT_MODEL=${OPENAI_CHAT_MODEL}
      - OPENAI_EMBED_MODEL=${OPENAI_EMBED_MODEL}

      - QDRANT_URL=http://qdrant:6333
      - QDRANT_COLLECTION=sf_kb
      - KB_PATH=/data/knowledge

      - POLL_SECONDS=${POLL_SECONDS}
      - TZ=${TZ}
    volumes:
      - /volume2/knowledge:/data/knowledge
      - /volume2/handled_cases:/data
    ports:
      - "8080:8080"
```
