package com.nby.agent.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service("ollamaClient")
public class OllamaClient implements LlmProvider {
  private static final Logger logger = LoggerFactory.getLogger(OllamaClient.class);
  
  private final String base = System.getenv().getOrDefault("OLLAMA_BASE","http://localhost:11434");
  private final String chatModel = System.getenv().getOrDefault("OLLAMA_CHAT_MODEL","llama3.1:8b");
  private final String embedModel = System.getenv().getOrDefault("OLLAMA_EMBED_MODEL","mxbai-embed-large");

  private final RestTemplate http = new RestTemplate();
  private final ObjectMapper om = new ObjectMapper();

  public double[] embed(String text) {
    logger.debug("Generating embeddings for text of length: {}", text.length());
    logger.debug("Using embed model: {}", embedModel);
    
    try {
      String payload = """
        {"model":"%s","input":%s,"options":{"truncate":true}}
      """.formatted(embedModel, om.writeValueAsString(text));
      
      HttpHeaders h = new HttpHeaders(); 
      h.setContentType(MediaType.APPLICATION_JSON);
      
      ResponseEntity<String> resp = http.postForEntity(base + "/api/embed", new HttpEntity<>(payload, h), String.class);
      JsonNode n = om.readTree(resp.getBody());
      JsonNode arr = n.get("embeddings").get(0);
      double[] v = new double[arr.size()];
      for (int i=0;i<arr.size();i++) v[i] = arr.get(i).asDouble();
      
      logger.debug("Generated embeddings with dimension: {}", v.length);
      return v;
    } catch (Exception e) {
      logger.error("Failed to generate embeddings", e);
      throw new RuntimeException(e);
    }
  }

  public String chat(String system, String user, int tokens) {
    logger.info("Starting chat completion with {} tokens", tokens);
    logger.debug("Using chat model: {}", chatModel);
    logger.debug("System prompt length: {}", system.length());
    logger.debug("User prompt length: {}", user.length());
    
    try {
      String payload = """
       {"model":"%s","stream":false,"options":{"num_predict":%d},"messages":[
          {"role":"system","content":%s},
          {"role":"user","content":%s}
       ]}
      """.formatted(chatModel, tokens, om.writeValueAsString(system), om.writeValueAsString(user));
      
      HttpHeaders h = new HttpHeaders(); 
      h.setContentType(MediaType.APPLICATION_JSON);
      
      ResponseEntity<String> resp = http.postForEntity(base + "/api/chat", new HttpEntity<>(payload,h), String.class);
      JsonNode n = om.readTree(resp.getBody());
      String response = n.get("message").get("content").asText();
      
      logger.info("Chat completion successful, response length: {}", response.length());
      logger.debug("Response: {}", response);
      return response;
    } catch (Exception e) {
      logger.error("Chat completion failed", e);
      throw new RuntimeException(e);
    }
  }
}
