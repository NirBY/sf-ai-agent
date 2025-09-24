package com.nby.agent.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service("openAIClient")
public class OpenAIClient implements LlmProvider {
  private static final Logger logger = LoggerFactory.getLogger(OpenAIClient.class);
  
  private final String base = System.getenv().getOrDefault("OPENAI_BASE","https://api.openai.com");
  private final String apiKey = System.getenv("OPENAI_API_KEY");
  private final String chatModel = System.getenv().getOrDefault("OPENAI_CHAT_MODEL","gpt-4o-mini");
  private final String embedModel = System.getenv().getOrDefault("OPENAI_EMBED_MODEL","text-embedding-3-large");
  private final RestTemplate http = new RestTemplate();
  private final ObjectMapper om = new ObjectMapper();

  private HttpEntity<String> entity(String json){
    HttpHeaders h = new HttpHeaders();
    h.setContentType(MediaType.APPLICATION_JSON);
    h.setBearerAuth(apiKey);
    return new HttpEntity<>(json,h);
  }

  @Override
  public double[] embed(String text) {
    logger.debug("Generating embeddings for text of length: {}", text.length());
    logger.debug("Using embed model: {}", embedModel);
    
    try {
      String payload = """
        {"model":%s,"input":%s}
      """.formatted(om.writeValueAsString(embedModel), om.writeValueAsString(text));
      
      ResponseEntity<String> r = http.postForEntity(base + "/v1/embeddings", entity(payload), String.class);
      JsonNode v = om.readTree(r.getBody()).get("data").get(0).get("embedding");
      double[] out = new double[v.size()];
      for (int i=0;i<v.size();i++) out[i]=v.get(i).asDouble();
      
      logger.debug("Generated embeddings with dimension: {}", out.length);
      return out;
    } catch(Exception e){
      logger.error("Failed to generate embeddings", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public String chat(String system, String user, int tokens) {
    logger.info("Starting OpenAI chat completion with {} tokens", tokens);
    logger.debug("Using chat model: {}", chatModel);
    logger.debug("System prompt length: {}", system.length());
    logger.debug("User prompt length: {}", user.length());
    
    try {
      // Chat Completions API: https://platform.openai.com/docs/api-reference/chat/completions
      String payload = """
      {
        "model": %s,
        "max_tokens": %d,
        "messages": [
          {"role":"system","content":%s},
          {"role":"user","content":%s}
        ]
      }
      """.formatted(
        om.writeValueAsString(chatModel),
        Math.max(1,tokens),
        om.writeValueAsString(system),
        om.writeValueAsString(user)
      );
      
      ResponseEntity<String> r = http.postForEntity(base + "/v1/chat/completions", entity(payload), String.class);
      JsonNode n = om.readTree(r.getBody());
      
      // Extract content from choices[0].message.content
      JsonNode choices = n.get("choices");
      if (choices != null && choices.isArray() && choices.size() > 0) {
        JsonNode message = choices.get(0).get("message");
        if (message != null) {
          JsonNode content = message.get("content");
          if (content != null && !content.isNull()) {
            String response = content.asText();
            logger.info("OpenAI chat completion successful, response length: {}", response.length());
            logger.debug("Response: {}", response);
            return response;
          }
        }
      }
      
      logger.error("No content found in OpenAI response");
      throw new RuntimeException("No content found in OpenAI response");
    } catch(Exception e){
      logger.error("OpenAI chat completion failed", e);
      throw new RuntimeException(e);
    }
  }
}
