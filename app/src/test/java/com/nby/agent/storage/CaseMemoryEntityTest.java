package com.nby.agent.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CaseMemoryEntityTest {

    @Test
    void testCaseMemoryEntity_Creation() {
        // Given
        String caseId = "test-case-001";
        long handledAtEpochMs = System.currentTimeMillis();
        
        // When
        CaseMemoryEntity entity = new CaseMemoryEntity(caseId, handledAtEpochMs);
        
        // Then
        assertNotNull(entity);
        assertEquals(caseId, entity.caseId());
        assertEquals(handledAtEpochMs, entity.handledAtEpochMs());
    }

    @Test
    void testCaseMemoryEntity_Equality() {
        // Given
        String caseId = "test-case-001";
        long handledAtEpochMs = System.currentTimeMillis();
        
        // When
        CaseMemoryEntity entity1 = new CaseMemoryEntity(caseId, handledAtEpochMs);
        CaseMemoryEntity entity2 = new CaseMemoryEntity(caseId, handledAtEpochMs);
        
        // Then
        assertEquals(entity1, entity2);
        assertEquals(entity1.hashCode(), entity2.hashCode());
    }

    @Test
    void testCaseMemoryEntity_Inequality() {
        // Given
        String caseId1 = "test-case-001";
        String caseId2 = "test-case-002";
        long handledAtEpochMs = System.currentTimeMillis();
        
        // When
        CaseMemoryEntity entity1 = new CaseMemoryEntity(caseId1, handledAtEpochMs);
        CaseMemoryEntity entity2 = new CaseMemoryEntity(caseId2, handledAtEpochMs);
        
        // Then
        assertNotEquals(entity1, entity2);
        assertNotEquals(entity1.hashCode(), entity2.hashCode());
    }

    @Test
    void testCaseMemoryEntity_ToString() {
        // Given
        String caseId = "test-case-001";
        long handledAtEpochMs = 1234567890L;
        
        // When
        CaseMemoryEntity entity = new CaseMemoryEntity(caseId, handledAtEpochMs);
        String toString = entity.toString();
        
        // Then
        assertNotNull(toString);
        assertTrue(toString.contains(caseId));
        assertTrue(toString.contains(String.valueOf(handledAtEpochMs)));
    }

    @Test
    void testCaseMemoryEntity_WithNullCaseId() {
        // Given
        String caseId = null;
        long handledAtEpochMs = System.currentTimeMillis();
        
        // When
        CaseMemoryEntity entity = new CaseMemoryEntity(caseId, handledAtEpochMs);
        
        // Then
        assertNull(entity.caseId());
        assertEquals(handledAtEpochMs, entity.handledAtEpochMs());
    }

    @Test
    void testCaseMemoryEntity_WithEmptyCaseId() {
        // Given
        String caseId = "";
        long handledAtEpochMs = System.currentTimeMillis();
        
        // When
        CaseMemoryEntity entity = new CaseMemoryEntity(caseId, handledAtEpochMs);
        
        // Then
        assertEquals("", entity.caseId());
        assertEquals(handledAtEpochMs, entity.handledAtEpochMs());
    }

    @Test
    void testCaseMemoryEntity_WithZeroTimestamp() {
        // Given
        String caseId = "test-case-001";
        long handledAtEpochMs = 0L;
        
        // When
        CaseMemoryEntity entity = new CaseMemoryEntity(caseId, handledAtEpochMs);
        
        // Then
        assertEquals(caseId, entity.caseId());
        assertEquals(0L, entity.handledAtEpochMs());
    }

    @Test
    void testCaseMemoryEntity_WithNegativeTimestamp() {
        // Given
        String caseId = "test-case-001";
        long handledAtEpochMs = -1L;
        
        // When
        CaseMemoryEntity entity = new CaseMemoryEntity(caseId, handledAtEpochMs);
        
        // Then
        assertEquals(caseId, entity.caseId());
        assertEquals(-1L, entity.handledAtEpochMs());
    }

    @Test
    void testCaseMemoryEntity_WithSpecialCharacters() {
        // Given
        String caseId = "test-case-with-special-chars-!@#$%^&*()";
        long handledAtEpochMs = System.currentTimeMillis();
        
        // When
        CaseMemoryEntity entity = new CaseMemoryEntity(caseId, handledAtEpochMs);
        
        // Then
        assertEquals(caseId, entity.caseId());
        assertEquals(handledAtEpochMs, entity.handledAtEpochMs());
    }

    @Test
    void testCaseMemoryEntity_WithUnicodeCharacters() {
        // Given
        String caseId = "test-case-עברית-中文-العربية";
        long handledAtEpochMs = System.currentTimeMillis();
        
        // When
        CaseMemoryEntity entity = new CaseMemoryEntity(caseId, handledAtEpochMs);
        
        // Then
        assertEquals(caseId, entity.caseId());
        assertEquals(handledAtEpochMs, entity.handledAtEpochMs());
    }
}
