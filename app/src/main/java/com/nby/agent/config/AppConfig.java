package com.nby.agent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
  private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
  
  @Bean
  public String tz(@Value("${TZ:Asia/Jerusalem}") String timezone) { 
    logger.info("Application timezone configured as: {}", timezone);
    return timezone;
  }
}
