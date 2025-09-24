package com.nby.agent.rag;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
public class TextExtractorService {

  public String fromFile(File f) {
    try (InputStream is = new FileInputStream(f)) {
      AutoDetectParser parser = new AutoDetectParser();
      BodyContentHandler handler = new BodyContentHandler(-1); // ללא מגבלת אורך
      Metadata metadata = new Metadata();
      parser.parse(is, handler, metadata);
      return handler.toString();
    } catch (Exception e) {
      throw new RuntimeException("Failed to extract text from file: " + f.getName(), e);
    }
  }

  public String fromUrl(String url) {
    try {
      // ננסה קודם עם Jsoup לניקוי מהיר של HTML
      Document doc = Jsoup.connect(url).userAgent("sf-ai-agent").get();
      String text = doc.text();
      if (text != null && !text.isBlank()) return text;

      // fallback: הורדת ה-HTML ופרסינג ע"י Tika
      try (InputStream is = URI.create(url).toURL().openStream()) {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata md = new Metadata();
        parser.parse(is, handler, md);
        return handler.toString();
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to fetch/extract from url: " + url, e);
    }
  }

  public static String safeSnippet(String text, int maxLen) {
    if (text == null) return "";
    String t = text.replaceAll("\\s+", " ").trim();
    if (t.length() <= maxLen) return t;
    return t.substring(0, maxLen) + "…";
  }
}
