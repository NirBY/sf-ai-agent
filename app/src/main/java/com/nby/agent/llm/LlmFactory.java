package com.nby.agent.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class LlmFactory {
  private static final Logger logger = LoggerFactory.getLogger(LlmFactory.class);
  
  @Bean
  @Primary
  public LlmProvider llmProvider(OllamaClient ollama, OpenAIClient openai) {
    String p = System.getProperty("LLM_PROVIDER", System.getenv().getOrDefault("LLM_PROVIDER","ollama")).toLowerCase();
    logger.info("Configuring LLM provider: {}", p);
    
    LlmProvider provider = switch (p) {
      case "openai" -> {
        logger.info("Using OpenAI as LLM provider");
        yield openai;
      }
      default -> {
        logger.info("Using Ollama as LLM provider (default)");
        yield ollama;
      }
    };
    
    return provider;
  }
}
