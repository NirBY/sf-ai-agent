package com.nby.agent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PromptTemplates {
  private static final Logger logger = LoggerFactory.getLogger(PromptTemplates.class);

  public static String systemPrompt() {
    logger.debug("Generating system prompt");
    // Hebrew-first system prompt; keep responses concise and actionable.
    String prompt = """
את/ה סוכן/ית תמיכה טכנית בסיילספורס. 
המטרה: 
1) לסכם בקצרה את הבעיה בעברית.
2) לנסח תשובת טיוטה (בעברית) מנומסת, מקצועית וברורה, כולל שלבי בדיקה/פתרון.
3) אם חסר מידע – הצע/י שאלות הבהרה. 
שמור/י על סגנון ידידותי, קצר ותכליתי.
אם יש מקורות רלוונטיים מהידע הארגוני (RAG) – שלב/י אותם לטובת תשובה מדויקת.
""";
    logger.debug("System prompt generated, length: {} characters", prompt.length());
    return prompt;
  }

  public static String userPrompt(String caseSubject, String caseDescription, String ragContext) {
    logger.debug("Generating user prompt for case: {}", caseSubject);
    logger.debug("Case description length: {} characters", caseDescription != null ? caseDescription.length() : 0);
    logger.debug("RAG context length: {} characters", ragContext != null ? ragContext.length() : 0);
    
    String prompt = """
פרטי המקרה:
כותרת: %s

תיאור:
%s

הקשר רלוונטי מהידע הארגוני (RAG):
%s

בקשה:
1) סיכום קצר בעברית (3–6 שורות).
2) תשובת טיוטה בעברית ללקוח/ה, כולל צעדים/בדיקות.
3) רשימת שאלות חסר אם צריך.
""".formatted(caseSubject, caseDescription == null ? "" : caseDescription, ragContext == null ? "" : ragContext);
    
    logger.debug("User prompt generated, total length: {} characters", prompt.length());
    return prompt;
  }
}
