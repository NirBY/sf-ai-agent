package com.nby.agent.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

    private MeterRegistry meterRegistry;
    private MetricsService metricsService;

    @BeforeEach
    void setUp() throws Exception {
        // Use real SimpleMeterRegistry instead of mock
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new MetricsService(meterRegistry);
    }

    @Test
    void testIncProcessed_IncrementsCounter() {
        // When
        metricsService.incProcessed();
        
        // Then
        Counter counter = meterRegistry.counter("sfagent_cases_processed");
        assertEquals(1.0, counter.count());
    }

    @Test
    void testIncSkippedHandled_IncrementsCounter() {
        // When
        metricsService.incSkippedHandled();
        
        // Then
        Counter counter = meterRegistry.counter("sfagent_cases_skipped_handled");
        assertEquals(1.0, counter.count());
    }

    @Test
    void testIncCommentPosted_IncrementsCounter() {
        // When
        metricsService.incCommentPosted();
        
        // Then
        Counter counter = meterRegistry.counter("sfagent_case_comments_posted");
        assertEquals(1.0, counter.count());
    }

    @Test
    void testIncSfError_IncrementsCounter() {
        // When
        metricsService.incSfError();
        
        // Then
        Counter counter = meterRegistry.counter("sfagent_errors_salesforce");
        assertEquals(1.0, counter.count());
    }

    @Test
    void testIncLlmError_IncrementsCounter() {
        // When
        metricsService.incLlmError();
        
        // Then
        Counter counter = meterRegistry.counter("sfagent_errors_llm");
        assertEquals(1.0, counter.count());
    }

    @Test
    void testIncRagError_IncrementsCounter() {
        // When
        metricsService.incRagError();
        
        // Then
        Counter counter = meterRegistry.counter("sfagent_errors_rag");
        assertEquals(1.0, counter.count());
    }

    @Test
    void testTimeSfList_RecordsTimer() throws Exception {
        // Given
        Callable<String> callable = () -> "test result";
        
        // When
        String result = metricsService.timeSfList(callable);
        
        // Then
        assertEquals("test result", result);
        Timer timer = meterRegistry.timer("sfagent_sf_list_seconds");
        assertEquals(1, timer.count());
    }

    @Test
    void testTimeSfFetchCase_RecordsTimer() throws Exception {
        // Given
        Callable<String> callable = () -> "test result";
        
        // When
        String result = metricsService.timeSfFetchCase(callable);
        
        // Then
        assertEquals("test result", result);
        Timer timer = meterRegistry.timer("sfagent_sf_fetch_case_seconds");
        assertEquals(1, timer.count());
    }

    @Test
    void testTimeSfPostCaseComment_RecordsTimer() throws Exception {
        // Given
        Callable<Void> callable = () -> null;
        
        // When
        Void result = metricsService.timeSfPostCaseComment(callable);
        
        // Then
        assertNull(result);
        Timer timer = meterRegistry.timer("sfagent_sf_post_case_comment_seconds");
        assertEquals(1, timer.count());
    }

    @Test
    void testTimeSfAuth_RecordsTimer() throws Exception {
        // Given
        Callable<String> callable = () -> "auth result";
        
        // When
        String result = metricsService.timeSfAuth(callable);
        
        // Then
        assertEquals("auth result", result);
        Timer timer = meterRegistry.timer("sfagent_sf_auth_seconds");
        assertEquals(1, timer.count());
    }

    @Test
    void testTimeQdrantGet_RecordsTimer() throws Exception {
        // Given
        Callable<String> callable = () -> "qdrant result";
        
        // When
        String result = metricsService.timeQdrantGet(callable);
        
        // Then
        assertEquals("qdrant result", result);
        Timer timer = meterRegistry.timer("sfagent_qdrant_get_seconds");
        assertEquals(1, timer.count());
    }

    @Test
    void testTimeQdrantPost_RecordsTimer() throws Exception {
        // Given
        Callable<String> callable = () -> "qdrant post result";
        
        // When
        String result = metricsService.timeQdrantPost(callable);
        
        // Then
        assertEquals("qdrant post result", result);
        Timer timer = meterRegistry.timer("sfagent_qdrant_post_seconds");
        assertEquals(1, timer.count());
    }

    @Test
    void testTimeQdrantPut_RecordsTimer() throws Exception {
        // Given
        Callable<Void> callable = () -> null;
        
        // When
        Void result = metricsService.timeQdrantPut(callable);
        
        // Then
        assertNull(result);
        Timer timer = meterRegistry.timer("sfagent_qdrant_put_seconds");
        assertEquals(1, timer.count());
    }

    @Test
    void testTimeDbQuery_RecordsTimer() throws Exception {
        // Given
        Callable<String> callable = () -> "db result";
        
        // When
        String result = metricsService.timeDbQuery(callable);
        
        // Then
        assertEquals("db result", result);
        Timer timer = meterRegistry.timer("sfagent_db_query_seconds");
        assertEquals(1, timer.count());
    }

    @Test
    void testTimeDbInsert_RecordsTimer() throws Exception {
        // Given
        Callable<Void> callable = () -> null;
        
        // When
        Void result = metricsService.timeDbInsert(callable);
        
        // Then
        assertNull(result);
        Timer timer = meterRegistry.timer("sfagent_db_insert_seconds");
        assertEquals(1, timer.count());
    }

    @Test
    void testTimeRag_RecordsTimer() throws Exception {
        // Given
        Callable<String> callable = () -> "rag result";
        
        // When
        String result = metricsService.timeRag(callable);
        
        // Then
        assertEquals("rag result", result);
        Timer timer = meterRegistry.timer("sfagent_rag_retrieve_seconds");
        assertEquals(1, timer.count());
    }

    @Test
    void testTimeLlmChat_RecordsTimer() throws Exception {
        // Given
        Callable<String> callable = () -> "llm chat result";
        
        // When
        String result = metricsService.timeLlmChat(callable);
        
        // Then
        assertEquals("llm chat result", result);
        Timer timer = meterRegistry.timer("sfagent_llm_chat_seconds");
        assertEquals(1, timer.count());
    }

    @Test
    void testTimeLlmEmbed_RecordsTimer() throws Exception {
        // Given
        Callable<double[]> callable = () -> new double[]{1.0, 2.0, 3.0};
        
        // When
        double[] result = metricsService.timeLlmEmbed(callable);
        
        // Then
        assertArrayEquals(new double[]{1.0, 2.0, 3.0}, result);
        Timer timer = meterRegistry.timer("sfagent_llm_embed_seconds");
        assertEquals(1, timer.count());
    }

    @Test
    void testTimerMethods_HandleExceptions() throws Exception {
        // Given
        Callable<String> callable = () -> {
            throw new RuntimeException("Test exception");
        };
        
        // When & Then
        assertThrows(RuntimeException.class, () -> {
            metricsService.timeSfList(callable);
        });
    }

    @Test
    void testTimerMethods_HandleCheckedExceptions() throws Exception {
        // Given
        Callable<String> callable = () -> {
            throw new Exception("Test checked exception");
        };
        
        // When & Then
        assertThrows(Exception.class, () -> {
            metricsService.timeSfList(callable);
        });
    }
}
