package com.nby.agent.salesforce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class SalesforceAuthService {
  private static final Logger logger = LoggerFactory.getLogger(SalesforceAuthService.class);
  
  private final RestTemplate http = new RestTemplate();
  private final ObjectMapper om = new ObjectMapper();

  private String accessToken;
  private String instanceUrl;

  public synchronized void login() {
    logger.info("Starting Salesforce authentication...");
    String loginUrl = System.getenv().getOrDefault("SF_LOGIN_URL","https://login.salesforce.com");
    String clientId = env("SF_CLIENT_ID");
    String clientSecret = env("SF_CLIENT_SECRET");
    String username = env("SF_USERNAME");
    String password = env("SF_PASSWORD"); // include security token appended

    logger.debug("Login URL: {}", loginUrl);
    logger.debug("Username: {}", username);
    logger.debug("Client ID: {}", clientId);

    String body = "grant_type=password"
        + "&client_id=" + enc(clientId)
        + "&client_secret=" + enc(clientSecret)
        + "&username=" + enc(username)
        + "&password=" + enc(password);

    HttpHeaders h = new HttpHeaders();
    h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    
    try {
      long startTime = System.currentTimeMillis();
      ResponseEntity<String> resp = http.postForEntity(loginUrl + "/services/oauth2/token", new HttpEntity<>(body, h), String.class);
      long duration = System.currentTimeMillis() - startTime;

      if (!resp.getStatusCode().is2xxSuccessful()) {
        logger.error("Salesforce login failed with status: {}", resp.getStatusCode());
        logger.error("Response body: {}", resp.getBody());
        throw new RuntimeException("Salesforce login failed: " + resp);
      }
      
      JsonNode n = om.readTree(resp.getBody());
      accessToken = n.get("access_token").asText();
      instanceUrl = n.get("instance_url").asText();
      
      logger.info("Salesforce authentication successful, duration: {}ms", duration);
      logger.debug("Instance URL: {}", instanceUrl);
      logger.debug("Access token length: {}", accessToken != null ? accessToken.length() : 0);
    } catch (Exception e) {
      logger.error("Salesforce authentication failed", e);
      throw new RuntimeException(e);
    }
  }

  public synchronized String bearer() {
    if (accessToken == null) {
      logger.debug("Access token is null, triggering login...");
      login();
    }
    return accessToken;
  }

  public synchronized String instanceUrl() {
    if (instanceUrl == null) {
      logger.debug("Instance URL is null, triggering login...");
      login();
    }
    return instanceUrl;
  }

  private static String env(String k) {
    String v = System.getenv(k);
    if (v == null || v.isBlank()) throw new IllegalArgumentException("Missing env: " + k);
    return v;
  }
  private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
}
