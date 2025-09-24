package com.nby.agent.salesforce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class CaseService {
  private static final Logger logger = LoggerFactory.getLogger(CaseService.class);
  
  private final SalesforceClient sf;
  private final ObjectMapper om = new ObjectMapper();
  private final String apiVersion = "/" + System.getenv().getOrDefault("SF_API_VERSION","v60.0");

  public CaseService(SalesforceClient sf) { this.sf = sf; }

  public JsonNode getCase(String caseId) {
    logger.debug("Retrieving case: {}", caseId);
    JsonNode result = sf.get(apiVersion + "/sobjects/Case/" + caseId);
    logger.debug("Case retrieved successfully: {}", caseId);
    return result;
  }

  public void postCaseComment(String caseId, String body) {
    logger.info("Posting comment to case: {}", caseId);
    logger.debug("Comment body length: {} characters", body.length());
    
    ObjectNode n = om.createObjectNode();
    n.put("ParentId", caseId);
    n.put("CommentBody", body);
    
    sf.post(apiVersion + "/sobjects/CaseComment", n.toString());
    logger.info("Comment posted successfully to case: {}", caseId);
  }

  /* ---------- SOQL helpers ---------- */

  public JsonNode query(String soql) {
    String encoded = URLEncoder.encode(soql, StandardCharsets.UTF_8);
    return sf.get(apiVersion + "/query?q=" + encoded);
  }

  /** Default: "All Open Cases" equivalent via SOQL. */
  public List<String> queryOpenCaseIds(int limit) {
    String soql = "SELECT Id FROM Case WHERE IsClosed = false ORDER BY CreatedDate DESC LIMIT " + Math.max(1, limit);
    logger.debug("Querying open cases with limit: {}", limit);
    logger.debug("SOQL: {}", soql);
    
    JsonNode res = query(soql);
    List<String> ids = new ArrayList<>();
    
    if (res != null && res.get("records") != null && res.get("records").isArray()) {
      for (JsonNode r : res.get("records")) {
        if (r.get("Id") != null) ids.add(r.get("Id").asText());
      }
    }
    
    logger.info("Found {} open cases", ids.size());
    return ids;
  }
}
