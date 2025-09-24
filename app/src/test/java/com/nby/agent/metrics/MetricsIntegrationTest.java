package com.nby.agent.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "logging.level.com.nby.agent=DEBUG",
    "logging.level.io.micrometer=DEBUG"
})
class MetricsIntegrationTest {

    private MeterRegistry meterRegistry;
    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new MetricsService(meterRegistry);
    }

    @Test
    void testMetricsService_CountersAreRegistered() {
        // Verify that all counters are registered
        assertNotNull(meterRegistry.find("sfagent_cases_processed").counter());
        assertNotNull(meterRegistry.find("sfagent_cases_skipped_handled").counter());
        assertNotNull(meterRegistry.find("sfagent_case_comments_posted").counter());
        assertNotNull(meterRegistry.find("sfagent_errors_salesforce").counter());
        assertNotNull(meterRegistry.find("sfagent_errors_llm").counter());
        assertNotNull(meterRegistry.find("sfagent_errors_rag").counter());
    }

    @Test
    void testMetricsService_TimersAreRegistered() {
        // Verify that all timers are registered
        assertNotNull(meterRegistry.find("sfagent_sf_fetch_case_seconds").timer());
        assertNotNull(meterRegistry.find("sfagent_sf_list_seconds").timer());
        assertNotNull(meterRegistry.find("sfagent_sf_post_case_comment_seconds").timer());
        assertNotNull(meterRegistry.find("sfagent_sf_auth_seconds").timer());
        assertNotNull(meterRegistry.find("sfagent_qdrant_get_seconds").timer());
        assertNotNull(meterRegistry.find("sfagent_qdrant_post_seconds").timer());
        assertNotNull(meterRegistry.find("sfagent_qdrant_put_seconds").timer());
        assertNotNull(meterRegistry.find("sfagent_db_query_seconds").timer());
        assertNotNull(meterRegistry.find("sfagent_db_insert_seconds").timer());
        assertNotNull(meterRegistry.find("sfagent_rag_retrieve_seconds").timer());
        assertNotNull(meterRegistry.find("sfagent_llm_chat_seconds").timer());
        assertNotNull(meterRegistry.find("sfagent_llm_embed_seconds").timer());
    }

    @Test
    void testCounterIncrements_UpdateValues() {
        // Given - initial values
        double initialProcessed = meterRegistry.find("sfagent_cases_processed").counter().count();
        double initialSkipped = meterRegistry.find("sfagent_cases_skipped_handled").counter().count();
        double initialComments = meterRegistry.find("sfagent_case_comments_posted").counter().count();

        // When - increment counters
        metricsService.incProcessed();
        metricsService.incSkippedHandled();
        metricsService.incCommentPosted();

        // Then - verify increments
        assertEquals(initialProcessed + 1, meterRegistry.find("sfagent_cases_processed").counter().count());
        assertEquals(initialSkipped + 1, meterRegistry.find("sfagent_cases_skipped_handled").counter().count());
        assertEquals(initialComments + 1, meterRegistry.find("sfagent_case_comments_posted").counter().count());
    }

    @Test
    void testErrorCounters_UpdateValues() {
        // Given - initial values
        double initialSfErrors = meterRegistry.find("sfagent_errors_salesforce").counter().count();
        double initialLlmErrors = meterRegistry.find("sfagent_errors_llm").counter().count();
        double initialRagErrors = meterRegistry.find("sfagent_errors_rag").counter().count();

        // When - increment error counters
        metricsService.incSfError();
        metricsService.incLlmError();
        metricsService.incRagError();

        // Then - verify increments
        assertEquals(initialSfErrors + 1, meterRegistry.find("sfagent_errors_salesforce").counter().count());
        assertEquals(initialLlmErrors + 1, meterRegistry.find("sfagent_errors_llm").counter().count());
        assertEquals(initialRagErrors + 1, meterRegistry.find("sfagent_errors_rag").counter().count());
    }

    @Test
    void testTimerMethods_RecordTiming() throws Exception {
        // Given
        Callable<String> callable = () -> {
            try {
                Thread.sleep(10); // Simulate some work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "test result";
        };

        // When
        String result = metricsService.timeSfList(callable);

        // Then
        assertEquals("test result", result);
        
        // Verify timer recorded the call
        var timer = meterRegistry.find("sfagent_sf_list_seconds").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
        assertTrue(timer.totalTime(TimeUnit.MILLISECONDS) >= 10);
    }

    @Test
    void testTimerMethods_HandleExceptions() {
        // Given
        Callable<String> callable = () -> {
            throw new RuntimeException("Test exception");
        };

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            metricsService.timeSfList(callable);
        });

        // Verify timer still recorded the call (even though it failed)
        var timer = meterRegistry.find("sfagent_sf_list_seconds").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void testTimerMethods_HandleVoidCallables() throws Exception {
        // Given
        Callable<Void> callable = () -> null;

        // When
        Void result = metricsService.timeSfPostCaseComment(callable);

        // Then
        assertNull(result);
        
        // Verify timer recorded the call
        var timer = meterRegistry.find("sfagent_sf_post_case_comment_seconds").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void testTimerMethods_HandleArrayReturnTypes() throws Exception {
        // Given
        Callable<double[]> callable = () -> new double[]{1.0, 2.0, 3.0};

        // When
        double[] result = metricsService.timeLlmEmbed(callable);

        // Then
        assertArrayEquals(new double[]{1.0, 2.0, 3.0}, result);
        
        // Verify timer recorded the call
        var timer = meterRegistry.find("sfagent_llm_embed_seconds").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void testMultipleTimerCalls_AccumulateCorrectly() throws Exception {
        // Given
        Callable<String> callable1 = () -> "result1";
        Callable<String> callable2 = () -> "result2";

        // When
        metricsService.timeSfList(callable1);
        metricsService.timeSfList(callable2);

        // Then
        var timer = meterRegistry.find("sfagent_sf_list_seconds").timer();
        assertNotNull(timer);
        assertEquals(2, timer.count());
    }

    @Test
    void testAllTimerMethods_WorkCorrectly() throws Exception {
        // Test all timer methods to ensure they work
        assertEquals("test", metricsService.timeSfList(() -> "test"));
        assertEquals("test", metricsService.timeSfFetchCase(() -> "test"));
        assertEquals("test", metricsService.timeSfAuth(() -> "test"));
        assertEquals("test", metricsService.timeQdrantGet(() -> "test"));
        assertEquals("test", metricsService.timeQdrantPost(() -> "test"));
        assertEquals("test", metricsService.timeDbQuery(() -> "test"));
        assertEquals("test", metricsService.timeRag(() -> "test"));
        assertEquals("test", metricsService.timeLlmChat(() -> "test"));
        
        assertNull(metricsService.timeSfPostCaseComment(() -> null));
        assertNull(metricsService.timeQdrantPut(() -> null));
        assertNull(metricsService.timeDbInsert(() -> null));
        
        assertArrayEquals(new double[]{1.0}, metricsService.timeLlmEmbed(() -> new double[]{1.0}));
    }
}
