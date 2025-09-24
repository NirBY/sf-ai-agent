package com.nby.agent.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nby.agent.config.PromptTemplates;
import com.nby.agent.llm.LlmProvider;
import com.nby.agent.llm.RagService;
import com.nby.agent.metrics.MetricsService;
import com.nby.agent.salesforce.CaseService;
import com.nby.agent.salesforce.ListViewService;
import com.nby.agent.storage.CaseMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaseWatcherTest {

    @Mock
    private ListViewService mockListViewService;
    
    @Mock
    private CaseService mockCaseService;
    
    @Mock
    private RagService mockRagService;
    
    @Mock
    private LlmProvider mockLlmProvider;
    
    @Mock
    private MetricsService mockMetricsService;
    
    @Mock
    private CaseMemoryRepository mockMemoryRepository;
    
    private CaseWatcher caseWatcher;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        
        // Mock metrics service to return the actual result with lenient stubbing
        lenient().when(mockMetricsService.timeSfList(any())).thenAnswer(invocation -> {
            try {
                return invocation.getArgument(0, Callable.class).call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        lenient().when(mockMetricsService.timeSfFetchCase(any())).thenAnswer(invocation -> {
            try {
                return invocation.getArgument(0, Callable.class).call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        lenient().when(mockMetricsService.timeRag(any())).thenAnswer(invocation -> {
            try {
                return invocation.getArgument(0, Callable.class).call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        lenient().when(mockMetricsService.timeLlmChat(any())).thenAnswer(invocation -> {
            try {
                return invocation.getArgument(0, Callable.class).call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        lenient().when(mockMetricsService.timeSfPostCaseComment(any())).thenAnswer(invocation -> {
            try {
                return invocation.getArgument(0, Callable.class).call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        // Mock memory repository
        lenient().when(mockMemoryRepository.isHandled(anyString())).thenReturn(false);
        
        // Mock LLM provider
        lenient().when(mockLlmProvider.chat(anyString(), anyString(), anyInt())).thenReturn("AI response");
        
        // Mock RAG service
        lenient().when(mockRagService.retrieve(anyString(), anyInt())).thenReturn("RAG context");
        
        // Mock case service
        JsonNode mockCase = objectMapper.createObjectNode()
            .put("Subject", "Test Case")
            .put("Description", "Test Description");
        lenient().when(mockCaseService.getCase(anyString())).thenReturn(mockCase);
        
        // Mock list view service
        JsonNode mockListView = objectMapper.createObjectNode()
            .set("records", objectMapper.createArrayNode()
                .add(objectMapper.createObjectNode().put("Id", "case1"))
                .add(objectMapper.createObjectNode().put("Id", "case2")));
        lenient().when(mockListViewService.getListViewResults(any())).thenReturn(mockListView);
        lenient().when(mockListViewService.getListViewResults(isNull())).thenReturn(mockListView);
        lenient().when(mockListViewService.findCaseListViewIdByLabel(anyString())).thenReturn("listview123");
        
        // Mock case service for SOQL
        lenient().when(mockCaseService.queryOpenCaseIds(anyInt())).thenReturn(List.of("case1", "case2"));
        
        caseWatcher = new CaseWatcher(
            mockListViewService, 
            mockCaseService, 
            mockRagService, 
            mockLlmProvider, 
            mockMetricsService,
            mockMemoryRepository
        );
    }

    @Test
    void testCaseWatcher_Constructor_InitializesCorrectly() throws Exception {
        // Then
        assertNotNull(caseWatcher);
    }

    @Test
    void testProcessViaListView_CallsMetricsService() throws Exception {
        // Given
        String listViewId = "listview123";
        
        // When
        caseWatcher.processViaListView();
        
        // Then
        verify(mockMetricsService).timeSfList(any());
        verify(mockListViewService).getListViewResults(isNull());
    }

    @Test
    void testProcessViaSoqlAllOpen_CallsMetricsService() throws Exception {
        // When
        caseWatcher.processViaSoqlAllOpen();
        
        // Then
        verify(mockMetricsService).timeSfList(any());
        verify(mockCaseService).queryOpenCaseIds(50);
    }

    @Test
    void testHandleCase_NewCase_ProcessesSuccessfully() throws Exception {
        // Given
        String caseId = "test-case-123";
        when(mockMemoryRepository.isHandled(caseId)).thenReturn(false);
        
        // When
        caseWatcher.handleCase(caseId);
        
        // Then
        verify(mockMetricsService).timeSfFetchCase(any());
        verify(mockMetricsService).timeRag(any());
        verify(mockMetricsService).timeLlmChat(any());
        verify(mockMetricsService).timeSfPostCaseComment(any());
        verify(mockMetricsService).incProcessed();
        verify(mockMetricsService).incCommentPosted();
        verify(mockMemoryRepository).markHandled(caseId);
    }

    @Test
    void testHandleCase_AlreadyHandled_SkipsProcessing() throws Exception {
        // Given
        String caseId = "test-case-123";
        when(mockMemoryRepository.isHandled(caseId)).thenReturn(true);
        
        // When
        caseWatcher.handleCase(caseId);
        
        // Then
        verify(mockMetricsService).incSkippedHandled();
        verify(mockCaseService, never()).getCase(anyString());
        verify(mockRagService, never()).retrieve(anyString(), anyInt());
        verify(mockLlmProvider, never()).chat(anyString(), anyString(), anyInt());
    }

    @Test
    void testHandleCase_RagFailure_IncrementsRagError() throws Exception {
        // Given
        String caseId = "test-case-123";
        when(mockMemoryRepository.isHandled(caseId)).thenReturn(false);
        when(mockRagService.retrieve(anyString(), anyInt())).thenThrow(new RuntimeException("RAG failed"));
        
        // When
        caseWatcher.handleCase(caseId);
        
        // Then
        verify(mockMetricsService).incRagError();
        verify(mockMetricsService, never()).incProcessed();
        verify(mockMemoryRepository, never()).markHandled(anyString());
    }

    @Test
    void testHandleCase_LlmFailure_IncrementsLlmError() throws Exception {
        // Given
        String caseId = "test-case-123";
        when(mockMemoryRepository.isHandled(caseId)).thenReturn(false);
        when(mockLlmProvider.chat(anyString(), anyString(), anyInt())).thenThrow(new RuntimeException("LLM failed"));
        
        // When
        caseWatcher.handleCase(caseId);
        
        // Then
        verify(mockMetricsService).incLlmError();
        verify(mockMetricsService, never()).incProcessed();
        verify(mockMemoryRepository, never()).markHandled(anyString());
    }

    @Test
    void testHandleCase_SalesforceFailure_IncrementsSfError() throws Exception {
        // Given
        String caseId = "test-case-123";
        when(mockMemoryRepository.isHandled(caseId)).thenReturn(false);
        doThrow(new RuntimeException("Salesforce failed")).when(mockCaseService).postCaseComment(anyString(), anyString());
        
        // When
        caseWatcher.handleCase(caseId);
        
        // Then
        verify(mockMetricsService).incSfError();
        verify(mockMetricsService, never()).incProcessed();
        verify(mockMemoryRepository, never()).markHandled(anyString());
    }

    @Test
    void testHandleCase_GeneratesCorrectComment() throws Exception {
        // Given
        String caseId = "test-case-123";
        when(mockMemoryRepository.isHandled(caseId)).thenReturn(false);
        when(mockLlmProvider.chat(anyString(), anyString(), anyInt())).thenReturn("AI generated response");
        
        // When
        caseWatcher.handleCase(caseId);
        
        // Then
        verify(mockCaseService).postCaseComment(eq(caseId), argThat(comment -> 
            comment.contains("סיכום + תשובת טיוטה (נוצר ע\"י AI Agent)") &&
            comment.contains("AI generated response")
        ));
    }

    @Test
    void testHandleCase_WithEmptyDescription() throws Exception {
        // Given
        String caseId = "test-case-123";
        when(mockMemoryRepository.isHandled(caseId)).thenReturn(false);
        
        JsonNode mockCase = objectMapper.createObjectNode()
            .put("Subject", "Test Case")
            .put("Description", "");
        when(mockCaseService.getCase(anyString())).thenReturn(mockCase);
        
        // When
        caseWatcher.handleCase(caseId);
        
        // Then
        verify(mockRagService).retrieve(eq("Test Case\n"), anyInt());
        verify(mockMetricsService).incProcessed();
    }

    @Test
    void testHandleCase_WithNullDescription() throws Exception {
        // Given
        String caseId = "test-case-123";
        when(mockMemoryRepository.isHandled(caseId)).thenReturn(false);
        
        JsonNode mockCase = objectMapper.createObjectNode()
            .put("Subject", "Test Case");
        when(mockCaseService.getCase(anyString())).thenReturn(mockCase);
        
        // When
        caseWatcher.handleCase(caseId);
        
        // Then
        verify(mockRagService).retrieve(eq("Test Case\n"), anyInt());
        verify(mockMetricsService).incProcessed();
    }
}
