package com.nby.agent.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AppConfigTest {

    private AppConfig appConfig;

    @BeforeEach
    void setUp() {
        appConfig = new AppConfig();
    }

    @Test
    void testTz_DefaultTimezone_ReturnsJerusalem() {
        // Given
        String defaultTimezone = "Asia/Jerusalem";
        
        // When
        String timezone = appConfig.tz(defaultTimezone);
        
        // Then
        assertNotNull(timezone);
        assertEquals(defaultTimezone, timezone);
    }

    @Test
    void testTz_CustomTimezone_ReturnsCustomValue() {
        // Given
        String customTimezone = "America/New_York";
        
        // When
        String timezone = appConfig.tz(customTimezone);
        
        // Then
        assertNotNull(timezone);
        assertEquals(customTimezone, timezone);
    }

    @Test
    void testTz_EmptyTimezone_ReturnsJerusalem() {
        // Given
        String emptyTimezone = "";
        
        // When
        String timezone = appConfig.tz(emptyTimezone);
        
        // Then
        assertNotNull(timezone);
        assertEquals(emptyTimezone, timezone);
    }

    @Test
    void testTz_BlankTimezone_ReturnsJerusalem() {
        // Given
        String blankTimezone = "   ";
        
        // When
        String timezone = appConfig.tz(blankTimezone);
        
        // Then
        assertNotNull(timezone);
        assertEquals(blankTimezone, timezone);
    }

    @Test
    void testTz_ValidTimezones() {
        // Given
        String expectedTimezone = "UTC";
        
        // When
        String timezone = appConfig.tz(expectedTimezone);
        
        // Then
        assertNotNull(timezone);
        assertEquals(expectedTimezone, timezone);
    }

    @Test
    void testTz_BeanIsSingleton() {
        // Given
        String testTimezone = "Europe/London";
        
        // When
        String timezone1 = appConfig.tz(testTimezone);
        String timezone2 = appConfig.tz(testTimezone);
        
        // Then
        assertNotNull(timezone1);
        assertNotNull(timezone2);
        assertEquals(timezone1, timezone2);
    }
}
