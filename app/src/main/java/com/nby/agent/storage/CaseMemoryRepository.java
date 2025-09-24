package com.nby.agent.storage;

import com.nby.agent.metrics.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

@Component
public class CaseMemoryRepository {
  private static final Logger logger = LoggerFactory.getLogger(CaseMemoryRepository.class);
  
  private final String url;
  private final MetricsService metrics;

  public CaseMemoryRepository(MetricsService metrics) {
    this.metrics = metrics;
    this.url = "jdbc:sqlite:" + System.getProperty("MEMORY_DB", System.getenv().getOrDefault("MEMORY_DB","/data/app/app.db"));
    logger.info("Initializing CaseMemoryRepository with database: {}", url);
    init();
  }

  private void init() {
    try (Connection c = DriverManager.getConnection(url);
         Statement s = c.createStatement()) {
      s.executeUpdate("CREATE TABLE IF NOT EXISTS handled_cases (case_id TEXT PRIMARY KEY, handled_at_ms INTEGER)");
      logger.info("Database table 'handled_cases' initialized successfully");
    } catch (SQLException e) {
      logger.error("Failed to initialize database", e);
      throw new RuntimeException(e);
    }
  }

  public boolean isHandled(String caseId) throws Exception {
    return metrics.timeDbQuery(() -> {
      try (Connection c = DriverManager.getConnection(url);
           PreparedStatement ps = c.prepareStatement("SELECT 1 FROM handled_cases WHERE case_id=?")) {
        ps.setString(1, caseId);
        boolean handled = ps.executeQuery().next();
        logger.debug("Case {} handled status: {}", caseId, handled);
        return handled;
      } catch (SQLException e) {
        logger.error("Failed to check if case is handled: {}", caseId, e);
        throw new RuntimeException(e);
      }
    });
  }

  public void markHandled(String caseId) throws Exception {
    metrics.timeDbInsert(() -> {
      try (Connection c = DriverManager.getConnection(url);
           PreparedStatement ps = c.prepareStatement("INSERT OR REPLACE INTO handled_cases(case_id, handled_at_ms) VALUES(?,?)")) {
        ps.setString(1, caseId);
        ps.setLong(2, System.currentTimeMillis());
        ps.executeUpdate();
        logger.debug("Marked case {} as handled", caseId);
        return null;
      } catch (SQLException e) {
        logger.error("Failed to mark case as handled: {}", caseId, e);
        throw new RuntimeException(e);
      }
    });
  }

  public Set<String> allIds() throws Exception {
    return metrics.timeDbQuery(() -> {
      Set<String> s = new HashSet<>();
      try (Connection c = DriverManager.getConnection(url);
           PreparedStatement ps = c.prepareStatement("SELECT case_id FROM handled_cases");
           ResultSet rs = ps.executeQuery()) {
        while (rs.next()) s.add(rs.getString(1));
        logger.debug("Retrieved {} handled case IDs from database", s.size());
      } catch (SQLException e) {
        logger.error("Failed to retrieve handled case IDs", e);
        throw new RuntimeException(e);
      }
      return s;
    });
  }
}
