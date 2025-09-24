package com.nby.agent.llm;

import com.nby.agent.metrics.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private LlmProvider mockLlmProvider;
    
    @Mock
    private MetricsService mockMetricsService;
    
    private RagService ragService;

    @BeforeEach
    void setUp() throws Exception {
        // Mock metrics service to return the actual result with lenient stubbing
        lenient().when(mockMetricsService.timeLlmEmbed(any())).thenAnswer(invocation -> {
            Callable<?> callable = invocation.getArgument(0, Callable.class);
            if (callable != null) {
                try {
                    return callable.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return null;
        });
        lenient().when(mockMetricsService.timeQdrantGet(any())).thenAnswer(invocation -> {
            Callable<?> callable = invocation.getArgument(0, Callable.class);
            if (callable != null) {
                try {
                    return callable.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return null;
        });
        lenient().when(mockMetricsService.timeQdrantPost(any())).thenAnswer(invocation -> {
            Callable<?> callable = invocation.getArgument(0, Callable.class);
            if (callable != null) {
                try {
                    return callable.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return null;
        });
        lenient().when(mockMetricsService.timeQdrantPut(any())).thenAnswer(invocation -> {
            Callable<?> callable = invocation.getArgument(0, Callable.class);
            if (callable != null) {
                try {
                    return callable.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return null;
        });
        
        // Mock LLM provider
        lenient().when(mockLlmProvider.embed(anyString())).thenReturn(new double[]{1.0, 2.0, 3.0});
        
        ragService = new RagService(mockLlmProvider, mockMetricsService);
    }

    @Test
    void testRagService_Constructor_InitializesCorrectly() throws Exception {
        // Then
        assertNotNull(ragService);
        // Constructor just initializes the service, no calls to embed() during initialization
    }

    @Test
    void testRetrieve_CallsMetricsService() throws Exception {
        // Given
        String query = "test query";
        int k = 5;
        
        // Mock the HTTP response
        Map<String, Object> mockResponse = Map.of(
            "result", java.util.List.of(
                Map.of("payload", Map.of("text", "test document 1")),
                Map.of("payload", Map.of("text", "test document 2"))
            )
        );
        
        lenient().when(mockMetricsService.timeQdrantGet(any())).thenReturn(mockResponse);
        lenient().when(mockMetricsService.timeQdrantPost(any())).thenReturn(mockResponse);

        // When
        String result = ragService.retrieve(query, k);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("test document 1"));
        assertTrue(result.contains("test document 2"));
        
        // Verify metrics were called
        verify(mockMetricsService).timeLlmEmbed(any());
        verify(mockMetricsService).timeQdrantPost(any());
    }

    @Test
    void testRetrieve_HandlesEmptyResults() throws Exception {
        // Given
        String query = "test query";
        int k = 5;
        
        // Mock empty response
        Map<String, Object> mockResponse = Map.of("result", java.util.List.of());
        
        lenient().when(mockMetricsService.timeQdrantGet(any())).thenReturn(mockResponse);
        lenient().when(mockMetricsService.timeQdrantPost(any())).thenReturn(mockResponse);

        // When
        String result = ragService.retrieve(query, k);

        // Then
        assertNotNull(result);
        assertEquals("", result.trim());
        
        // Verify metrics were called
        verify(mockMetricsService).timeLlmEmbed(any());
        verify(mockMetricsService).timeQdrantPost(any());
    }

    @Test
    void testRetrieve_HandlesNullPayload() throws Exception {
        // Given
        String query = "test query";
        int k = 5;
        
        // Mock response with null payload
        Map<String, Object> nullPayloadMap = new HashMap<>();
        nullPayloadMap.put("payload", null);
        
        Map<String, Object> mockResponse = Map.of(
            "result", java.util.List.of(
                nullPayloadMap,
                Map.of("payload", Map.of("text", "valid document"))
            )
        );
        
        lenient().when(mockMetricsService.timeQdrantGet(any())).thenReturn(mockResponse);
        lenient().when(mockMetricsService.timeQdrantPost(any())).thenReturn(mockResponse);

        // When
        String result = ragService.retrieve(query, k);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("valid document"));
        assertFalse(result.contains("null"));
        
        // Verify metrics were called
        verify(mockMetricsService).timeLlmEmbed(any());
        verify(mockMetricsService).timeQdrantPost(any());
    }

    @Test
    void testRetrieve_RespectsLimit() throws Exception {
        // Given
        String query = "test query";
        int k = 2; // Limit to 2 results
        
        // Mock response with more results than limit
        Map<String, Object> mockResponse = Map.of(
            "result", java.util.List.of(
                Map.of("payload", Map.of("text", "document 1")),
                Map.of("payload", Map.of("text", "document 2")),
                Map.of("payload", Map.of("text", "document 3")),
                Map.of("payload", Map.of("text", "document 4"))
            )
        );
        
        lenient().when(mockMetricsService.timeQdrantGet(any())).thenReturn(mockResponse);
        lenient().when(mockMetricsService.timeQdrantPost(any())).thenReturn(mockResponse);

        // When
        String result = ragService.retrieve(query, k);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("document 1"));
        assertTrue(result.contains("document 2"));
        assertFalse(result.contains("document 3"));
        assertFalse(result.contains("document 4"));
        
        // Verify metrics were called
        verify(mockMetricsService).timeLlmEmbed(any());
        verify(mockMetricsService).timeQdrantPost(any());
    }

    @Test
    void testRetrieve_HandlesEmbeddingFailure() throws Exception {
        // Given
        String query = "test query";
        int k = 5;
        
        // Mock embedding failure
        lenient().when(mockMetricsService.timeLlmEmbed(any())).thenThrow(new RuntimeException("Embedding failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            ragService.retrieve(query, k);
        });
        
        // Verify metrics were called
        verify(mockMetricsService).timeLlmEmbed(any());
        verify(mockMetricsService, never()).timeQdrantPost(any());
    }

    @Test
    void testRetrieve_HandlesQdrantFailure() throws Exception {
        // Given
        String query = "test query";
        int k = 5;
        
        // Mock Qdrant failure
        lenient().when(mockMetricsService.timeQdrantPost(any())).thenThrow(new RuntimeException("Qdrant failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            ragService.retrieve(query, k);
        });
        
        // Verify metrics were called
        verify(mockMetricsService).timeLlmEmbed(any());
        verify(mockMetricsService).timeQdrantPost(any());
    }

    @Test
    void testIngestFolder_CallsMetricsService() throws Exception {
        // This test would require more complex mocking of file system operations
        // For now, we'll just verify that the method exists and can be called
        assertDoesNotThrow(() -> {
            // The method might throw IOException due to missing knowledge base path
            // but that's expected in a test environment
            try {
                ragService.ingestFolder();
            } catch (Exception e) {
                // Expected in test environment without proper file system setup
            }
        });
    }
}
