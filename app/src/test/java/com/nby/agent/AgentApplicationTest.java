package com.nby.agent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "SF_LOGIN_URL=https://test.salesforce.com",
    "SF_CLIENT_ID=test_client_id",
    "SF_CLIENT_SECRET=test_client_secret",
    "SF_USERNAME=test@example.com",
    "SF_PASSWORD=test_password",
    "SF_API_VERSION=v60.0",
    "SF_CASE_LISTVIEW_LABEL=Test List View",
    "OLLAMA_BASE=http://localhost:11434",
    "OLLAMA_CHAT_MODEL=llama3.1:8b",
    "OLLAMA_EMBED_MODEL=mxbai-embed-large",
    "QDRANT_URL=http://localhost:6333",
    "QDRANT_COLLECTION=test_collection",
    "KB_PATH=/tmp/test_kb",
    "MEMORY_DB=/tmp/test.db",
    "POLL_SECONDS=60",
    "TZ=UTC"
})
class AgentApplicationTest {

    @Test
    void contextLoads() {
        // This test verifies that the Spring application context loads successfully
        // with all the required beans and configurations
    }
}
