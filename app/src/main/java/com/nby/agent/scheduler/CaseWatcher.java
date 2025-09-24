package com.nby.agent.scheduler;

import com.nby.agent.config.PromptTemplates;
import com.nby.agent.llm.LlmProvider;            // <— if you use the provider interface
// If you kept OllamaClient directly, replace with: import com.nby.agent.llm.OllamaClient;
import com.nby.agent.llm.RagService;
import com.nby.agent.salesforce.CaseService;
import com.nby.agent.salesforce.ListViewService;
import com.nby.agent.storage.CaseMemoryRepository;
import com.nby.agent.metrics.MetricsService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CaseWatcher {
  private static final Logger logger = LoggerFactory.getLogger(CaseWatcher.class);

  private final ListViewService listViews;
  private final CaseService cases;
  private final RagService rag;
  private final LlmProvider llm;                     // or OllamaClient if not using provider
  private final CaseMemoryRepository memory;
  private final MetricsService metrics;

  private final String listViewLabel = System.getenv().getOrDefault("SF_CASE_LISTVIEW_LABEL","").trim();
  private String listViewId = null;
  private boolean useListView = false;

  public CaseWatcher(ListViewService listViews, CaseService cases, RagService rag, LlmProvider llm, MetricsService metrics, CaseMemoryRepository memory) {
    this.listViews = listViews;
    this.cases = cases;
    this.rag = rag;
    this.llm = llm;
    this.metrics = metrics;
    this.memory = memory;

    logger.info("Initializing CaseWatcher...");
    logger.info("ListView label: '{}'", listViewLabel);

    // Decide mode
    if (!listViewLabel.isBlank()) {
      try {
        this.listViewId = listViews.findCaseListViewIdByLabel(listViewLabel);
        this.useListView = true;
        logger.info("Using List View: '{}' (id={})", listViewLabel, listViewId);
      } catch (Exception e) {
        logger.warn("List View not available ({}). Fallback to SOQL: All Open Cases.", e.getMessage());
        this.useListView = false;
      }
    } else {
      logger.info("SF_CASE_LISTVIEW_LABEL empty. Using SOQL default: All Open Cases.");
      this.useListView = false;
    }
  }

  @Scheduled(initialDelay = 5000, fixedDelayString = "#{T(java.lang.Integer).parseInt(systemEnvironment['POLL_SECONDS']?:'60')*1000}")
  public void tick() {
    logger.debug("Starting scheduled case check...");
    try {
      if (useListView) {
        logger.debug("Processing cases via List View");
        processViaListView();
      } else {
        logger.debug("Processing cases via SOQL");
        processViaSoqlAllOpen();
      }
    } catch (Exception e) {
      logger.error("Error during scheduled case check", e);
    }
  }

  void processViaListView() throws Exception {
    JsonNode res = metrics.timeSfList(() -> listViews.getListViewResults(listViewId));
    JsonNode rows = res.get("records");
    if (rows == null || !rows.isArray()) {
      logger.debug("No records found in list view");
      return;
    }
    
    int totalCases = rows.size();
    logger.info("Found {} cases in list view", totalCases);
    
    for (JsonNode r : rows) {
      String caseId = r.get("Id").asText();
      handleCase(caseId);
    }
  }

  void processViaSoqlAllOpen() throws Exception {
    // Pull a reasonable window (newest first)
    List<String> ids = metrics.timeSfList(() -> cases.queryOpenCaseIds(50));
    logger.info("Found {} open cases via SOQL", ids.size());
    
    for (String caseId : ids) {
      handleCase(caseId);
    }
  }

  void handleCase(String caseId) throws Exception {
    if (memory.isHandled(caseId)) {
      logger.debug("Case {} already handled, skipping", caseId);
      metrics.incSkippedHandled();
      return;
    }

    logger.info("Processing new case: {}", caseId);
    
    try {
      JsonNode full = metrics.timeSfFetchCase(() -> cases.getCase(caseId));
      String subj = text(full, "Subject");
      String desc = text(full, "Description");
      
      logger.debug("Case subject: {}", subj);
      logger.debug("Case description length: {} characters", desc.length());

      logger.debug("Retrieving RAG context...");
      String ragCtx;
      try {
        ragCtx = metrics.timeRag(() -> rag.retrieve(subj + "\n" + desc, 5));
        logger.debug("RAG context length: {} characters", ragCtx.length());
      } catch (Exception e) {
        metrics.incRagError();
        logger.error("RAG retrieval failed for case: {}", caseId, e);
        throw e;
      }

      String sys = PromptTemplates.systemPrompt();
      String usr = PromptTemplates.userPrompt(subj, desc, ragCtx);

      logger.info("Generating AI response for case: {}", caseId);
      String answer;
      try {
        answer = metrics.timeLlmChat(() -> llm.chat(sys, usr, 600));
      } catch (Exception e) {
        metrics.incLlmError();
        logger.error("LLM chat failed for case: {}", caseId, e);
        throw e;
      }
      
      String comment = "סיכום + תשובת טיוטה (נוצר ע\"י AI Agent):\n\n" + answer;

      logger.info("Posting comment to case: {}", caseId);
      try {
        metrics.timeSfPostCaseComment(() -> {
          cases.postCaseComment(caseId, comment);
          return null; // Return null since postCaseComment returns void
        });
        memory.markHandled(caseId);
        metrics.incProcessed();
        metrics.incCommentPosted();
        logger.info("Successfully processed case: {}", caseId);
      } catch (Exception e) {
        metrics.incSfError();
        logger.error("Failed to post comment to case: {}", caseId, e);
        throw e;
      }
    } catch (Exception e) {
      logger.error("Failed to process case: {}", caseId, e);
    }
  }

  private static String text(JsonNode n, String field) {
    JsonNode v = n.get(field);
    return v == null || v.isNull() ? "" : v.asText();
  }
}
