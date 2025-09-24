package com.nby.agent.salesforce;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ListViewService {
  private static final Logger logger = LoggerFactory.getLogger(ListViewService.class);
  
  private final SalesforceClient sf;
  private final String apiVersion = "/" + System.getenv().getOrDefault("SF_API_VERSION","v60.0");

  public ListViewService(SalesforceClient sf) { this.sf = sf; }

  public String findCaseListViewIdByLabel(String label) {
    logger.debug("Searching for list view with label: {}", label);
    
    if (label == null || label.isBlank()) {
      logger.warn("No list view label provided");
      throw new IllegalArgumentException("No list view label provided");
    }
    
    JsonNode list = sf.get(apiVersion + "/sobjects/Case/listviews");
    if (list == null || list.get("listviews") == null) {
      logger.error("No list views returned for Case");
      throw new RuntimeException("No list views returned for Case");
    }
    
    for (JsonNode lv : list.get("listviews")) {
      String currentLabel = lv.get("label").asText();
      if (label.equalsIgnoreCase(currentLabel)) {
        String listViewId = lv.get("id").asText();
        logger.info("Found list view '{}' with ID: {}", label, listViewId);
        return listViewId;
      }
    }
    
    logger.error("List view not found by label: {}", label);
    throw new RuntimeException("List view not found by label: " + label);
  }

  public JsonNode getListViewResults(String listViewId) {
    logger.debug("Getting list view results for ID: {}", listViewId);
    JsonNode result = sf.get(apiVersion + "/sobjects/Case/listviews/" + listViewId + "/results");
    logger.debug("List view results retrieved successfully for ID: {}", listViewId);
    return result;
  }
}
