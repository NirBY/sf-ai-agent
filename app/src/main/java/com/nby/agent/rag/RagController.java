package com.nby.agent.rag;

import com.nby.agent.llm.RagService;
import com.nby.agent.metrics.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/rag")
public class RagController {
  private static final Logger logger = LoggerFactory.getLogger(RagController.class);
  
  private final DocumentIngestService ingest;
  private final RagService rag;
  private final MetricsService metrics;

  public RagController(DocumentIngestService ingest, RagService rag, MetricsService metrics) {
    this.ingest = ingest;
    this.rag = rag;
    this.metrics = metrics;
  }

  @PostMapping(path="/ingest/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Map<String,Object> upload(@RequestParam("file") MultipartFile file) throws Exception {
    logger.info("RAG upload request: file={}, size={} bytes", file.getOriginalFilename(), file.getSize());
    long startTime = System.currentTimeMillis();
    
    try {
      String path = metrics.timeRagIngest(() -> {
        try {
          return ingest.ingestFile(file.getOriginalFilename(), file.getInputStream());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });
      long duration = System.currentTimeMillis() - startTime;
      logger.info("RAG upload completed successfully: file={}, duration={}ms", file.getOriginalFilename(), duration);
      return Map.of("ok", true, "storedAt", path);
    } catch (Exception e) {
      metrics.incRagError();
      logger.error("RAG upload failed: file={}", file.getOriginalFilename(), e);
      throw e;
    }
  }

  @PostMapping("/ingest/url")
  public Map<String,Object> ingestUrl(@RequestParam("url") String url) {
    logger.info("RAG URL ingest request: url={}", url);
    long startTime = System.currentTimeMillis();
    
    try {
      metrics.timeRagIngest(() -> {
        try {
          ingest.ingestUrl(url);
          return null;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });
      long duration = System.currentTimeMillis() - startTime;
      logger.info("RAG URL ingest completed successfully: url={}, duration={}ms", url, duration);
      return Map.of("ok", true, "url", url);
    } catch (Exception e) {
      metrics.incRagError();
      logger.error("RAG URL ingest failed: url={}", url, e);
      throw new RuntimeException("Failed to ingest URL: " + url, e);
    }
  }

  @PostMapping("/reindex")
  public Map<String,Object> reindex() throws Exception {
    logger.info("RAG reindex request");
    long startTime = System.currentTimeMillis();
    
    try {
      metrics.timeRagRetrieve(() -> {
        try {
          rag.reindexAll();
          return null;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });
      long duration = System.currentTimeMillis() - startTime;
      logger.info("RAG reindex completed successfully: duration={}ms", duration);
      return Map.of("ok", true);
    } catch (Exception e) {
      metrics.incRagError();
      logger.error("RAG reindex failed", e);
      throw e;
    }
  }

  public record QueryRequest(String question, Integer topK, Integer maxTokens) {}
  public record Source(String name, String uri, String snippet, double score) {}
  public record AnswerResponse(String answer, List<Source> sources) {}

  @PostMapping("/query")
  public AnswerResponse query(@RequestBody QueryRequest req) {
    logger.info("RAG query request: question='{}', topK={}, maxTokens={}", 
                req.question(), req.topK(), req.maxTokens());
    long startTime = System.currentTimeMillis();
    
    try {
      int k = Optional.ofNullable(req.topK()).orElse(5);
      int tokens = Optional.ofNullable(req.maxTokens()).orElse(800);

      // נבצע חיפוש לקבלת מקורות, ואז תשובה בעברית
      List<com.nby.agent.llm.RagService.SearchHit> hits = metrics.timeRagRetrieve(() -> {
        try {
          return rag.search(req.question(), k);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });
      List<Source> sources = new ArrayList<>();
      for (com.nby.agent.llm.RagService.SearchHit h : hits) {
        sources.add(new Source(
          h.name(),
          (h.uri()==null || h.uri().isBlank()) ? null : h.uri(),
          TextExtractorService.safeSnippet(h.snippet(), 300),
          h.score()
        ));
      }
      String answer = metrics.timeLlmChat(() -> {
        try {
          return rag.answerInHebrew(req.question(), k, tokens);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });
      
      long duration = System.currentTimeMillis() - startTime;
      logger.info("RAG query completed successfully: question='{}', sources={}, duration={}ms", 
                  req.question(), sources.size(), duration);
      
      return new AnswerResponse(answer, sources);
    } catch (Exception e) {
      metrics.incRagError();
      logger.error("RAG query failed: question='{}'", req.question(), e);
      throw new RuntimeException("Failed to process RAG query: " + req.question(), e);
    }
  }
}
