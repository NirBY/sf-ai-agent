package com.nby.agent.llm;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class LlmFactoryTest {

    private LlmFactory llmFactory;
    private OllamaClient mockOllamaClient;
    private OpenAIClient mockOpenAIClient;

    @BeforeEach
    void setUp() {
        // Clear system property before each test
        System.clearProperty("LLM_PROVIDER");
        llmFactory = new LlmFactory();
        mockOllamaClient = mock(OllamaClient.class);
        mockOpenAIClient = mock(OpenAIClient.class);
    }

    @AfterEach
    void tearDown() {
        // Clear system property after each test
        System.clearProperty("LLM_PROVIDER");
    }

    @Test
    void testLlmProvider_DefaultProvider_ReturnsOllama() {
        // Given
        System.setProperty("LLM_PROVIDER", "ollama");
        
        // When
        LlmProvider provider = llmFactory.llmProvider(mockOllamaClient, mockOpenAIClient);
        
        // Then
        assertNotNull(provider);
        assertSame(mockOllamaClient, provider);
    }

    @Test
    void testLlmProvider_OpenAIProvider_ReturnsOpenAI() {
        // Given
        System.setProperty("LLM_PROVIDER", "openai");
        
        // When
        LlmProvider provider = llmFactory.llmProvider(mockOllamaClient, mockOpenAIClient);
        
        // Then
        assertNotNull(provider);
        assertSame(mockOpenAIClient, provider);
    }

    @Test
    void testLlmProvider_UnknownProvider_ReturnsOllama() {
        // Given
        System.setProperty("LLM_PROVIDER", "unknown");
        
        // When
        LlmProvider provider = llmFactory.llmProvider(mockOllamaClient, mockOpenAIClient);
        
        // Then
        assertNotNull(provider);
        assertSame(mockOllamaClient, provider);
    }

    @Test
    void testLlmProvider_EmptyProvider_ReturnsOllama() {
        // Given
        System.setProperty("LLM_PROVIDER", "");
        
        // When
        LlmProvider provider = llmFactory.llmProvider(mockOllamaClient, mockOpenAIClient);
        
        // Then
        assertNotNull(provider);
        assertSame(mockOllamaClient, provider);
    }

    @Test
    void testLlmProvider_CaseInsensitive_OpenAI() {
        // Given
        System.setProperty("LLM_PROVIDER", "OPENAI");
        
        // When
        LlmProvider provider = llmFactory.llmProvider(mockOllamaClient, mockOpenAIClient);
        
        // Then
        assertNotNull(provider);
        assertSame(mockOpenAIClient, provider);
    }

    @Test
    void testLlmProvider_CaseInsensitive_Ollama() {
        // Given
        System.setProperty("LLM_PROVIDER", "OLLAMA");
        
        // When
        LlmProvider provider = llmFactory.llmProvider(mockOllamaClient, mockOpenAIClient);
        
        // Then
        assertNotNull(provider);
        assertSame(mockOllamaClient, provider);
    }

    @Test
    void testLlmProvider_MixedCase_OpenAI() {
        // Given
        System.setProperty("LLM_PROVIDER", "OpenAI");
        
        // When
        LlmProvider provider = llmFactory.llmProvider(mockOllamaClient, mockOpenAIClient);
        
        // Then
        assertNotNull(provider);
        assertSame(mockOpenAIClient, provider);
    }

    @Test
    void testLlmProvider_NoPropertySet_ReturnsOllama() {
        // Given
        System.clearProperty("LLM_PROVIDER");
        
        // When
        LlmProvider provider = llmFactory.llmProvider(mockOllamaClient, mockOpenAIClient);
        
        // Then
        assertNotNull(provider);
        assertSame(mockOllamaClient, provider);
    }
}
