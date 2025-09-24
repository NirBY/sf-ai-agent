package com.nby.agent.salesforce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class SalesforceClient {
  private static final Logger logger = LoggerFactory.getLogger(SalesforceClient.class);
  
  private final SalesforceAuthService auth;
  private final RestTemplate http = new RestTemplate();
  private final ObjectMapper om = new ObjectMapper();

  public SalesforceClient(SalesforceAuthService auth) { this.auth = auth; }

  private HttpHeaders headers() {
    HttpHeaders h = new HttpHeaders();
    h.setBearerAuth(auth.bearer());
    h.setContentType(MediaType.APPLICATION_JSON);
    return h;
  }

  public JsonNode get(String path) {
    String url = auth.instanceUrl() + path;
    logger.debug("Making GET request to: {}", url);
    
    try {
      long startTime = System.currentTimeMillis();
      ResponseEntity<String> resp = http.exchange(url, HttpMethod.GET, new HttpEntity<>(headers()), String.class);
      long duration = System.currentTimeMillis() - startTime;
      
      if (resp.getStatusCode().value() == 401) {
        logger.warn("Received 401 Unauthorized, re-authenticating and retrying...");
        auth.login();
        return get(path);
      }
      
      logger.debug("GET request successful, status: {}, duration: {}ms", resp.getStatusCode(), duration);
      return om.readTree(resp.getBody());
    } catch (Exception e) {
      logger.error("GET request failed for path: {}", path, e);
      throw new RuntimeException(e);
    }
  }

  public JsonNode post(String path, String json) {
    String url = auth.instanceUrl() + path;
    logger.debug("Making POST request to: {}", url);
    logger.debug("Request body length: {} characters", json.length());
    
    try {
      long startTime = System.currentTimeMillis();
      ResponseEntity<String> resp = http.exchange(url, HttpMethod.POST, new HttpEntity<>(json, headers()), String.class);
      long duration = System.currentTimeMillis() - startTime;
      
      if (resp.getStatusCode().value() == 401) {
        logger.warn("Received 401 Unauthorized, re-authenticating and retrying...");
        auth.login();
        return post(path, json);
      }
      
      logger.debug("POST request successful, status: {}, duration: {}ms", resp.getStatusCode(), duration);
      return om.readTree(resp.getBody());
    } catch (Exception e) {
      logger.error("POST request failed for path: {}", path, e);
      throw new RuntimeException(e);
    }
  }
}
