package com.nby.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AgentApplication {
  private static final Logger logger = LoggerFactory.getLogger(AgentApplication.class);
  
  public static void main(String[] args) {
    logger.info("Starting Salesforce AI Agent...");
    logger.info("Java version: {}", System.getProperty("java.version"));
    logger.info("Spring Boot version: {}", org.springframework.boot.SpringBootVersion.getVersion());
    
    try {
      SpringApplication.run(AgentApplication.class, args);
      logger.info("Salesforce AI Agent started successfully!");
    } catch (Exception e) {
      logger.error("Failed to start Salesforce AI Agent", e);
      System.exit(1);
    }
  }
}
