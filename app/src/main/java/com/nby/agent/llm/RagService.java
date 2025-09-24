package com.nby.agent.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nby.agent.metrics.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class RagService {
  private static final Logger logger = LoggerFactory.getLogger(RagService.class);
  
  private final LlmProvider llmProvider;
  private final MetricsService metrics;
  private final String qdrantUrl = System.getenv().getOrDefault("QDRANT_URL","http://localhost:6333");
  private final String collection = System.getenv().getOrDefault("QDRANT_COLLECTION","sf_kb");
  private final String kbPath = System.getenv().getOrDefault("KB_PATH","/data/knowledge");
  private final ObjectMapper om = new ObjectMapper();

  public RagService(LlmProvider llmProvider, MetricsService metrics) { 
    this.llmProvider = llmProvider;
    this.metrics = metrics; 
    logger.info("Initializing RAG service with LLM provider: {}", llmProvider.getClass().getSimpleName());
    logger.info("Qdrant URL: {}", qdrantUrl);
    logger.info("Knowledge base path: {}", kbPath);
    logger.info("Collection name: {}", collection);
    initCollection(); 
    ingestIfEmpty(); 
  }

  private void initCollection() {
    try {
      logger.debug("Initializing Qdrant collection: {}", collection);
      // create collection if not exists
      String payload = """
        {"vectors":{"size":1024,"distance":"Cosine"}}""";
      // We optimistically create; ignore if already there.
      httpPut("/collections/" + collection, payload);
      logger.info("Qdrant collection '{}' initialized successfully", collection);
    } catch (Exception e) {
      logger.warn("Failed to initialize collection (may already exist): {}", e.getMessage());
    }
  }

  private void ingestIfEmpty() {
    try {
      logger.debug("Checking if collection needs ingestion...");
      Map<?,?> coll = httpGet("/collections/" + collection);
      Map<?,?> status = (Map<?,?>) coll.get("result");
      // if empty points – try ingest
      ingestFolder();
    } catch (Exception e) {
      logger.warn("Failed to check collection status or ingest documents: {}", e.getMessage());
    }
  }

  public void ingestFolder() throws IOException, Exception {
    logger.info("Starting knowledge base ingestion from: {}", kbPath);
    Path dir = Paths.get(kbPath);
    if (!Files.isDirectory(dir)) {
      logger.warn("Knowledge base path is not a directory: {}", kbPath);
      return;
    }

    List<String> docs = new ArrayList<>();
    try (var stream = Files.walk(dir)) {
      stream.filter(Files::isRegularFile).forEach(p -> {
        try {
          String content = Files.readString(p, StandardCharsets.UTF_8);
          logger.debug("Processing file: {} ({} characters)", p.getFileName(), content.length());
          // simple chunking
          for (String chunk : chunk(content, 1000, 200)) {
            docs.add(chunk);
          }
        } catch (Exception e) {
          logger.warn("Failed to process file: {}", p.getFileName(), e);
        }
      });
    }
    
    logger.info("Found {} document chunks to ingest", docs.size());
    
    // upsert into Qdrant
    int id = 1;
    List<Map<String,Object>> points = new ArrayList<>();
    for (String d : docs) {
      logger.debug("Generating embeddings for chunk {}/{} using {}", id, docs.size(), llmProvider.getClass().getSimpleName());
      double[] v = metrics.timeLlmEmbed(() -> llmProvider.embed(d));
      Map<String,Object> p = new HashMap<>();
      p.put("id", id++);
      p.put("vector", v);
      Map<String, Object> payload = new HashMap<>();
      payload.put("text", d);
      p.put("payload", payload);
      points.add(p);
    }
    
    Map<String,Object> up = new HashMap<>();
    up.put("points", points);
    httpPut("/collections/" + collection + "/points?wait=true", om.writeValueAsString(up));
    logger.info("Successfully ingested {} document chunks into Qdrant", points.size());
  }

  public String retrieve(String query, int k) throws Exception {
    logger.debug("Retrieving {} relevant documents for query of length: {} using {}", k, query.length(), llmProvider.getClass().getSimpleName());
    
    double[] v = metrics.timeLlmEmbed(() -> llmProvider.embed(query));
    String payload = """
      {"vector":%s,"limit":%d,"with_payload":true}
    """.formatted(Arrays.toString(v), k);
    
    Map<?,?> res = httpPost("/collections/" + collection + "/points/search", payload);
    StringBuilder sb = new StringBuilder();
    List<?> r = (List<?>) res.get("result");
    int count = 0;
    
    for (Object o : r) {
      Map<?,?> m = (Map<?,?>) o;
      Map<?,?> pl = (Map<?,?>) m.get("payload");
      if (pl != null && pl.get("text") != null) {
        String text = (String) pl.get("text");
        sb.append(++count).append(") ").append(text).append("\n\n");
        logger.debug("Retrieved document {}/{}: {} characters", count, k, text.length());
      }
      if (count >= k) break;
    }
    
    logger.info("Retrieved {} relevant documents for RAG context", count);
    return sb.toString();
  }

  /* ---------- tiny HTTP helpers ---------- */
  private Map<?,?> httpGet(String path) throws IOException, Exception {
    logger.debug("Making GET request to Qdrant: {}", path);
    return metrics.timeQdrantGet(() -> {
      try {
        HttpURLConnection c = (HttpURLConnection)URI.create(qdrantUrl + path).toURL().openConnection();
        c.setRequestMethod("GET");
        Map<?,?> result = om.readValue(c.getInputStream(), Map.class);
        logger.debug("Qdrant GET request successful: {}", path);
        return result;
      } catch (Exception e) {
        logger.error("Qdrant GET request failed: {}", path, e);
        throw new RuntimeException(e);
      }
    });
  }
  private Map<?,?> httpPost(String path, String json) throws Exception {
    logger.debug("Making POST request to Qdrant: {}", path);
    return metrics.timeQdrantPost(() -> {
      try {
        HttpURLConnection c = (HttpURLConnection)URI.create(qdrantUrl + path).toURL().openConnection();
        c.setRequestMethod("POST");
        c.setRequestProperty("Content-Type", "application/json");
        c.setDoOutput(true);
        try (OutputStream os = c.getOutputStream()) { os.write(json.getBytes(StandardCharsets.UTF_8)); }
        Map<?,?> result = om.readValue(c.getInputStream(), Map.class);
        logger.debug("Qdrant POST request successful: {}", path);
        return result;
      } catch (Exception e) { 
        logger.error("Qdrant POST request failed: {}", path, e);
        throw new RuntimeException(e); 
      }
    });
  }
  private void httpPut(String path, String json) throws Exception {
    logger.debug("Making PUT request to Qdrant: {}", path);
    metrics.timeQdrantPut(() -> {
      try {
        HttpURLConnection c = (HttpURLConnection)URI.create(qdrantUrl + path).toURL().openConnection();
        c.setRequestMethod("PUT");
        c.setRequestProperty("Content-Type", "application/json");
        c.setDoOutput(true);
        try (OutputStream os = c.getOutputStream()) { os.write(json.getBytes(StandardCharsets.UTF_8)); }
        c.getInputStream().close();
        logger.debug("Qdrant PUT request successful: {}", path);
        return null;
      } catch (Exception e) { 
        logger.error("Qdrant PUT request failed: {}", path, e);
        throw new RuntimeException(e); 
      }
    });
  }

  private static List<String> chunk(String text, int size, int overlap) {
    List<String> out = new ArrayList<>();
    int i = 0; 
    while (i < text.length()) {
      int end = Math.min(text.length(), i + size);
      out.add(text.substring(i, end));
      i = end - overlap;
      if (i < 0) i = 0;
      if (i >= text.length()) break;
    }
    return out;
  }

    /* ====== Public API ====== */

    public void reindexAll() throws IOException, Exception {
      Path dir = Paths.get(kbPath);
      if (!Files.isDirectory(dir)) return;
      List<QPoint> points = new ArrayList<>();
      try (var stream = Files.walk(dir)) {
        stream.filter(Files::isRegularFile).forEach(p -> {
          try {
            String content = Files.readString(p, StandardCharsets.UTF_8);
            String sourceId = p.toAbsolutePath().toString();
            String name = p.getFileName().toString();
            points.addAll(chunksToPoints(content, sourceId, name, "file", null));
          } catch (Exception ignore) {}
        });
      }
      upsert(points);
    }

    public void ingestText(String content, String sourceId, String name, String sourceType, String uri) throws Exception {
      List<QPoint> points = chunksToPoints(content, sourceId, name, sourceType, uri);
      upsert(points);
    }

    public List<SearchHit> search(String query, int k) throws Exception {
      double[] v = llmProvider.embed(query);
      String payload = """
        {"vector":%s,"limit":%d,"with_payload":true}
      """.formatted(Arrays.toString(v), k);
      Map<?,?> res = httpPost("/collections/" + collection + "/points/search", payload);
      List<SearchHit> out = new ArrayList<>();
      List<?> r = (List<?>) res.get("result");
      if (r == null) return out;
      for (Object o : r) {
        @SuppressWarnings("unchecked")
        Map<String,Object> m = (Map<String,Object>) o;
        double score = ((Number)m.getOrDefault("score", 0)).doubleValue();
        @SuppressWarnings("unchecked")
        Map<String,Object> pl = (Map<String,Object>) m.get("payload");
        if (pl == null) continue;
        out.add(new SearchHit(
          String.valueOf(pl.getOrDefault("source_id","")),
          String.valueOf(pl.getOrDefault("name","")),
          String.valueOf(pl.getOrDefault("source_type","")),
          String.valueOf(pl.getOrDefault("uri","")),
          String.valueOf(pl.getOrDefault("text","")),
          score
        ));
      }
      return out;
    }

    
  public String answerInHebrew(String question, int k, int tokens) throws Exception {
    List<SearchHit> hits = search(question, k);
    StringBuilder ctx = new StringBuilder();
    int i = 1;
    for (SearchHit h : hits) {
      ctx.append(i++).append(") [").append(h.name()).append("] ")
         .append(h.snippet()).append("\n\n");
    }
    String system = """
את/ה עוזר/ת תמיכה בעברית. ענה/י בעברית בלבד.
שלב/י בתשובה מידע רק מתוך "ההקשר" להלן. אם אין מידע מתאים—ציין/י שאין מספיק מידע.
החזר/י תשובה מובנית וברורה, וצרף/י רשימת מקורות בסוף.
""";
    String user = """
שאלה:
%s

ההקשר (קטעים רלוונטיים ממסמכים ואתרים):
%s

בקשה:
1) תשובה קצרה ומדויקת בעברית.
2) אם רלוונטי – צעדי פתרון/בדיקה.
3) "מקורות": רשום/י שם מקור ו-URI (אם יש) מהם נלקח המידע.
""".formatted(question, ctx.toString());

    return llmProvider.chat(system, user, tokens);
  }

   /* ====== Ingest helpers ====== */

   private List<QPoint> chunksToPoints(String content, String sourceId, String name, String sourceType, String uri) {
    List<QPoint> out = new ArrayList<>();
    List<String> chunks = chunk(content, 1000, 200);
    int idx = 0;
    for (String ch : chunks) {
      double[] v = llmProvider.embed(ch);
      Map<String,Object> payload = new HashMap<>();
      payload.put("text", ch);
      payload.put("source_id", sourceId);
      payload.put("name", name);
      payload.put("source_type", sourceType);
      if (uri != null) payload.put("uri", uri);
      payload.put("chunk_index", idx++);
      out.add(new QPoint(UUID.randomUUID().toString(), v, payload));
    }
    return out;
  }

  private void upsert(List<QPoint> points) throws Exception {
    if (points.isEmpty()) return;
    List<Map<String,Object>> arr = new ArrayList<>();
    for (QPoint p : points) {
      Map<String,Object> m = new HashMap<>();
      m.put("id", p.id());
      m.put("vector", p.vector());
      m.put("payload", p.payload());
      arr.add(m);
    }
    String body;
    try {
      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("points", arr);
      body = om.writeValueAsString(requestBody);
    } catch (Exception e) { throw new RuntimeException(e); }
    httpPut("/collections/" + collection + "/points?wait=true", body);
  }

  public record SearchHit(String sourceId, String name, String sourceType, String uri, String snippet, double score) {}
  public record QPoint(String id, double[] vector, Map<String,Object> payload) {}

}
