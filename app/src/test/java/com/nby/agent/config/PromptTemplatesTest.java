package com.nby.agent.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PromptTemplatesTest {

    @Test
    void testSystemPrompt() {
        // Given & When
        String systemPrompt = PromptTemplates.systemPrompt();
        
        // Then
        assertNotNull(systemPrompt);
        assertFalse(systemPrompt.trim().isEmpty());
        assertTrue(systemPrompt.contains("סוכן/ית תמיכה טכנית"));
        assertTrue(systemPrompt.contains("לסכם בקצרה"));
        assertTrue(systemPrompt.contains("תשובת טיוטה"));
        assertTrue(systemPrompt.contains("שאלות הבהרה"));
    }

    @Test
    void testUserPromptWithValidInputs() {
        // Given
        String caseSubject = "בעיית התחברות";
        String caseDescription = "המשתמש לא מצליח להתחבר למערכת";
        String ragContext = "מידע רלוונטי מהידע הארגוני";
        
        // When
        String userPrompt = PromptTemplates.userPrompt(caseSubject, caseDescription, ragContext);
        
        // Then
        assertNotNull(userPrompt);
        assertFalse(userPrompt.trim().isEmpty());
        assertTrue(userPrompt.contains(caseSubject));
        assertTrue(userPrompt.contains(caseDescription));
        assertTrue(userPrompt.contains(ragContext));
        assertTrue(userPrompt.contains("פרטי המקרה"));
        assertTrue(userPrompt.contains("בקשה"));
    }

    @Test
    void testUserPromptWithNullInputs() {
        // Given
        String caseSubject = "בדיקת null";
        String caseDescription = null;
        String ragContext = null;
        
        // When
        String userPrompt = PromptTemplates.userPrompt(caseSubject, caseDescription, ragContext);
        
        // Then
        assertNotNull(userPrompt);
        assertFalse(userPrompt.trim().isEmpty());
        assertTrue(userPrompt.contains(caseSubject));
        assertTrue(userPrompt.contains("כותרת: " + caseSubject));
        assertTrue(userPrompt.contains("תיאור:\n"));
        assertTrue(userPrompt.contains("הקשר רלוונטי מהידע הארגוני (RAG):\n"));
    }

    @Test
    void testUserPromptWithEmptyInputs() {
        // Given
        String caseSubject = "";
        String caseDescription = "";
        String ragContext = "";
        
        // When
        String userPrompt = PromptTemplates.userPrompt(caseSubject, caseDescription, ragContext);
        
        // Then
        assertNotNull(userPrompt);
        assertFalse(userPrompt.trim().isEmpty());
        assertTrue(userPrompt.contains("כותרת: "));
        assertTrue(userPrompt.contains("תיאור:\n"));
        assertTrue(userPrompt.contains("הקשר רלוונטי מהידע הארגוני (RAG):\n"));
    }

    @Test
    void testUserPromptFormatting() {
        // Given
        String caseSubject = "Test Case";
        String caseDescription = "Test Description";
        String ragContext = "Test RAG Context";
        
        // When
        String userPrompt = PromptTemplates.userPrompt(caseSubject, caseDescription, ragContext);
        
        // Then
        assertTrue(userPrompt.contains("כותרת: Test Case"));
        assertTrue(userPrompt.contains("תיאור:\nTest Description"));
        assertTrue(userPrompt.contains("הקשר רלוונטי מהידע הארגוני (RAG):\nTest RAG Context"));
    }
}
