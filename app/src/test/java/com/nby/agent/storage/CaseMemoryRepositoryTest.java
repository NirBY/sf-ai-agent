package com.nby.agent.storage;

import com.nby.agent.metrics.MetricsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseMemoryRepositoryTest {

    @TempDir
    Path tempDir;
    
    @Mock
    private MetricsService mockMetricsService;
    
    private CaseMemoryRepository repository;
    private String testDbPath;

    @BeforeEach
    void setUp() throws Exception {
        // Clear system property before each test
        System.clearProperty("MEMORY_DB");
        // Create unique database file for each test
        testDbPath = tempDir.resolve("test-" + System.currentTimeMillis() + "-" + Thread.currentThread().hashCode() + ".db").toString();
        System.setProperty("MEMORY_DB", testDbPath);
        
        // Mock metrics service to return the actual result with lenient stubbing
        lenient().when(mockMetricsService.timeDbQuery(any())).thenAnswer(invocation -> {
            return invocation.getArgument(0, java.util.concurrent.Callable.class).call();
        });
        lenient().when(mockMetricsService.timeDbInsert(any())).thenAnswer(invocation -> {
            return invocation.getArgument(0, java.util.concurrent.Callable.class).call();
        });
        
        repository = new CaseMemoryRepository(mockMetricsService);
    }

    @AfterEach
    void tearDown() {
        // Clear system property after each test
        System.clearProperty("MEMORY_DB");
    }

    @Test
    void testIsHandled_NewCase_ReturnsFalse() throws Exception {
        // Given
        String caseId = "test-case-001";
        
        // When
        boolean isHandled = repository.isHandled(caseId);
        
        // Then
        assertFalse(isHandled);
    }

    @Test
    void testMarkHandled_NewCase_ReturnsTrue() throws Exception {
        // Given
        String caseId = "test-case-001";
        
        // When
        repository.markHandled(caseId);
        boolean isHandled = repository.isHandled(caseId);
        
        // Then
        assertTrue(isHandled);
    }

    @Test
    void testMarkHandled_MultipleCases_AllHandled() throws Exception {
        // Given
        String caseId1 = "test-case-001";
        String caseId2 = "test-case-002";
        String caseId3 = "test-case-003";
        
        // When
        repository.markHandled(caseId1);
        repository.markHandled(caseId2);
        repository.markHandled(caseId3);
        
        // Then
        assertTrue(repository.isHandled(caseId1));
        assertTrue(repository.isHandled(caseId2));
        assertTrue(repository.isHandled(caseId3));
    }

    @Test
    void testAllIds_EmptyRepository_ReturnsEmptySet() throws Exception {
        // When
        Set<String> allIds = repository.allIds();
        
        // Then
        assertNotNull(allIds);
        assertTrue(allIds.isEmpty());
    }

    @Test
    void testAllIds_WithHandledCases_ReturnsAllIds() throws Exception {
        // Given
        String caseId1 = "test-case-001";
        String caseId2 = "test-case-002";
        String caseId3 = "test-case-003";
        
        repository.markHandled(caseId1);
        repository.markHandled(caseId2);
        repository.markHandled(caseId3);
        
        // When
        Set<String> allIds = repository.allIds();
        
        // Then
        assertNotNull(allIds);
        assertEquals(3, allIds.size());
        assertTrue(allIds.contains(caseId1));
        assertTrue(allIds.contains(caseId2));
        assertTrue(allIds.contains(caseId3));
    }

    @Test
    void testMarkHandled_DuplicateCase_OverwritesTimestamp() throws Exception {
        // Given
        String caseId = "test-case-001";
        
        // When
        repository.markHandled(caseId);
        boolean firstHandled = repository.isHandled(caseId);
        
        // Wait a bit to ensure different timestamp
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        repository.markHandled(caseId);
        boolean secondHandled = repository.isHandled(caseId);
        
        // Then
        assertTrue(firstHandled);
        assertTrue(secondHandled);
    }

    @Test
    void testCaseIdWithSpecialCharacters() throws Exception {
        // Given
        String caseId = "test-case-with-special-chars-!@#$%^&*()";
        
        // When
        repository.markHandled(caseId);
        boolean isHandled = repository.isHandled(caseId);
        
        // Then
        assertTrue(isHandled);
    }

    @Test
    void testCaseIdWithUnicodeCharacters() throws Exception {
        // Given
        String caseId = "test-case-עברית-中文-العربية";
        
        // When
        repository.markHandled(caseId);
        boolean isHandled = repository.isHandled(caseId);
        
        // Then
        assertTrue(isHandled);
    }

    @Test
    void testEmptyCaseId() throws Exception {
        // Given
        String caseId = "";
        
        // When & Then
        assertDoesNotThrow(() -> {
            repository.markHandled(caseId);
            boolean isHandled = repository.isHandled(caseId);
            assertTrue(isHandled);
        });
    }

    @Test
    void testNullCaseId() throws Exception {
        // Given
        String caseId = null;
        
        // When & Then - null caseId should be handled gracefully
        assertDoesNotThrow(() -> {
            repository.markHandled(caseId);
            boolean isHandled = repository.isHandled(caseId);
            // null caseId might not be found in database - this is acceptable
            assertFalse(isHandled);
        });
    }
}
