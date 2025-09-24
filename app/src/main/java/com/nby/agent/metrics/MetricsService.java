package com.nby.agent.metrics;

import io.micrometer.core.instrument.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {
  private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);
  
  private final Counter casesProcessed;
  private final Counter casesSkippedHandled;
  private final Counter caseCommentsPosted;
  private final Counter errorsSalesforce;
  private final Counter errorsLlm;
  private final Counter errorsRag;

  private final Timer sfFetchCaseTimer;
  private final Timer sfListTimer;
  private final Timer sfPostCaseCommentTimer;
  private final Timer sfAuthTimer;
  private final Timer qdrantGetTimer;
  private final Timer qdrantPostTimer;
  private final Timer qdrantPutTimer;
  private final Timer dbQueryTimer;
  private final Timer dbInsertTimer;
  private final Timer ragRetrieveTimer;
  private final Timer ragIngestTimer;
  private final Timer llmChatTimer;
  private final Timer llmEmbedTimer;

  public MetricsService(MeterRegistry registry) {
    casesProcessed = Counter.builder("sfagent_cases_processed").description("Cases processed").register(registry);
    casesSkippedHandled = Counter.builder("sfagent_cases_skipped_handled").description("Cases skipped - already handled").register(registry);
    caseCommentsPosted = Counter.builder("sfagent_case_comments_posted").description("CaseComments posted to Salesforce").register(registry);

    errorsSalesforce = Counter.builder("sfagent_errors_salesforce").description("Salesforce errors").register(registry);
    errorsLlm = Counter.builder("sfagent_errors_llm").description("LLM errors").register(registry);
    errorsRag = Counter.builder("sfagent_errors_rag").description("RAG errors").register(registry);

    sfFetchCaseTimer = Timer.builder("sfagent_sf_fetch_case_seconds").description("Time to fetch a Case").register(registry);
    sfListTimer = Timer.builder("sfagent_sf_list_seconds").description("Time to fetch list view / SOQL").register(registry);
    sfPostCaseCommentTimer = Timer.builder("sfagent_sf_post_case_comment_seconds").description("Time to post case comment to Salesforce").register(registry);
    sfAuthTimer = Timer.builder("sfagent_sf_auth_seconds").description("Time for Salesforce authentication").register(registry);
    qdrantGetTimer = Timer.builder("sfagent_qdrant_get_seconds").description("Time for Qdrant GET requests").register(registry);
    qdrantPostTimer = Timer.builder("sfagent_qdrant_post_seconds").description("Time for Qdrant POST requests").register(registry);
    qdrantPutTimer = Timer.builder("sfagent_qdrant_put_seconds").description("Time for Qdrant PUT requests").register(registry);
    dbQueryTimer = Timer.builder("sfagent_db_query_seconds").description("Time for database queries").register(registry);
    dbInsertTimer = Timer.builder("sfagent_db_insert_seconds").description("Time for database inserts").register(registry);
    ragRetrieveTimer = Timer.builder("sfagent_rag_retrieve_seconds").description("Time to retrieve from vector DB").register(registry);
    ragIngestTimer = Timer.builder("sfagent_rag_ingest_seconds").description("Time to ingest documents into RAG").register(registry);
    llmChatTimer = Timer.builder("sfagent_llm_chat_seconds").description("Time for LLM chat call").register(registry);
    llmEmbedTimer = Timer.builder("sfagent_llm_embed_seconds").description("Time for LLM embeddings").register(registry);
  }

  public void incProcessed() { 
    casesProcessed.increment(); 
    logger.debug("Incremented cases processed counter");
  }
  public void incSkippedHandled() { 
    casesSkippedHandled.increment(); 
    logger.debug("Incremented cases skipped counter");
  }
  public void incCommentPosted() { 
    caseCommentsPosted.increment(); 
    logger.debug("Incremented case comments posted counter");
  }
  public void incSfError() { 
    errorsSalesforce.increment(); 
    logger.warn("Incremented Salesforce errors counter");
  }
  public void incLlmError() { 
    errorsLlm.increment(); 
    logger.warn("Incremented LLM errors counter");
  }
  public void incRagError() { 
    errorsRag.increment(); 
    logger.warn("Incremented RAG errors counter");
  }

  public <T> T timeSfList(java.util.concurrent.Callable<T> c) throws Exception {
    return sfListTimer.recordCallable(c);
  }
  public <T> T timeSfFetchCase(java.util.concurrent.Callable<T> c) throws Exception {
    return sfFetchCaseTimer.recordCallable(c);
  }
  public <T> T timeSfPostCaseComment(java.util.concurrent.Callable<T> c) throws Exception {
    return sfPostCaseCommentTimer.recordCallable(c);
  }
  public <T> T timeSfAuth(java.util.concurrent.Callable<T> c) throws Exception {
    return sfAuthTimer.recordCallable(c);
  }
  public <T> T timeQdrantGet(java.util.concurrent.Callable<T> c) throws Exception {
    return qdrantGetTimer.recordCallable(c);
  }
  public <T> T timeQdrantPost(java.util.concurrent.Callable<T> c) throws Exception {
    return qdrantPostTimer.recordCallable(c);
  }
  public <T> T timeQdrantPut(java.util.concurrent.Callable<T> c) throws Exception {
    return qdrantPutTimer.recordCallable(c);
  }
  public <T> T timeDbQuery(java.util.concurrent.Callable<T> c) throws Exception {
    return dbQueryTimer.recordCallable(c);
  }
  public <T> T timeDbInsert(java.util.concurrent.Callable<T> c) throws Exception {
    return dbInsertTimer.recordCallable(c);
  }
  public <T> T timeRag(java.util.concurrent.Callable<T> c) throws Exception {
    return ragRetrieveTimer.recordCallable(c);
  }
  public <T> T timeLlmChat(java.util.concurrent.Callable<T> c) throws Exception {
    return llmChatTimer.recordCallable(c);
  }
  public <T> T timeLlmEmbed(java.util.concurrent.Callable<T> c) throws Exception {
    return llmEmbedTimer.recordCallable(c);
  }
  public <T> T timeRagIngest(java.util.concurrent.Callable<T> c) throws Exception {
    return ragIngestTimer.recordCallable(c);
  }
  public <T> T timeRagRetrieve(java.util.concurrent.Callable<T> c) throws Exception {
    return ragRetrieveTimer.recordCallable(c);
  }

}
