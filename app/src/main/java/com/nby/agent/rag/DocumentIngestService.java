package com.nby.agent.rag;

import com.nby.agent.llm.RagService;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;

@Service
public class DocumentIngestService {
  private final RagService rag;
  private final TextExtractorService extractor;
  private final String kbPath = System.getenv().getOrDefault("KB_PATH","/data/knowledge");

  public DocumentIngestService(RagService rag, TextExtractorService extractor) {
    this.rag = rag;
    this.extractor = extractor;
  }

  public String ingestFile(String originalName, InputStream content) throws IOException, Exception {
    Path destDir = Paths.get(kbPath);
    Files.createDirectories(destDir);
    Path dest = destDir.resolve(System.currentTimeMillis() + "_" + sanitize(originalName));
    Files.copy(content, dest, StandardCopyOption.REPLACE_EXISTING);

    String text = extractor.fromFile(dest.toFile());
    rag.ingestText(text, dest.toAbsolutePath().toString(), originalName, "file", null);
    return dest.toString();
  }

  public void ingestUrl(String url) throws Exception {
    String text = extractor.fromUrl(url);
    // sourceId=name יכול להיות ה-URL עצמו
    rag.ingestText(text, url, url, "url", url);
  }

  private static String sanitize(String s) { return s.replaceAll("[^\\w\\-.]+","_"); }
}
