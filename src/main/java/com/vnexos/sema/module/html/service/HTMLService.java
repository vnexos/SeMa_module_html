package com.vnexos.sema.module.html.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import com.vnexos.sema.context.ModuleServerContext;
import com.vnexos.sema.loader.annotations.Service;
import com.vnexos.sema.module.html.HTMLMainClass;

@Service
public class HTMLService {
  private static final Map<String, String> MIME_TYPES = new HashMap<>();

  static {
    // Text
    MIME_TYPES.put("txt", "text/plain");
    MIME_TYPES.put("html", "text/html");
    MIME_TYPES.put("htm", "text/html");
    MIME_TYPES.put("css", "text/css");
    MIME_TYPES.put("js", "text/javascript");
    MIME_TYPES.put("json", "application/json");
    MIME_TYPES.put("xml", "application/xml");

    // Images
    MIME_TYPES.put("jpg", "image/jpeg");
    MIME_TYPES.put("jpeg", "image/jpeg");
    MIME_TYPES.put("png", "image/png");
    MIME_TYPES.put("gif", "image/gif");
    MIME_TYPES.put("svg", "image/svg+xml");
    MIME_TYPES.put("ico", "image/x-icon");
    MIME_TYPES.put("webp", "image/webp");

    // Documents
    MIME_TYPES.put("pdf", "application/pdf");
    MIME_TYPES.put("doc", "application/msword");
    MIME_TYPES.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    MIME_TYPES.put("xls", "application/vnd.ms-excel");
    MIME_TYPES.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    MIME_TYPES.put("ppt", "application/vnd.ms-powerpoint");
    MIME_TYPES.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");

    // Archives
    MIME_TYPES.put("zip", "application/zip");
    MIME_TYPES.put("rar", "application/x-rar-compressed");
    MIME_TYPES.put("tar", "application/x-tar");
    MIME_TYPES.put("gz", "application/gzip");

    // Audio/Video
    MIME_TYPES.put("mp3", "audio/mpeg");
    MIME_TYPES.put("wav", "audio/wav");
    MIME_TYPES.put("mp4", "video/mp4");
    MIME_TYPES.put("webm", "video/webm");

    // Default fallback
    MIME_TYPES.put("bin", "application/octet-stream");
  }

  public HTMLService() {
  }

  public void initResourceFile(String fileName) {
    ModuleServerContext context = HTMLMainClass.context;
    File file = new File(context.joinPath(fileName));

    if (file.exists())
      return;

    File parentDir = file.getParentFile();
    if (parentDir != null && !parentDir.exists()) {
      parentDir.mkdirs();
    }

    try (InputStream is = getClass().getResourceAsStream(fileName)) {
      if (is == null) {
        throw new FileNotFoundException("Resource not found: " + fileName);
      }

      Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException ex) {
      context.log(ex);
    }
  }

  public byte[] readFromFile(String fileName) throws IOException {
    ModuleServerContext context = HTMLMainClass.context;
    File file = new File(context.joinPath(fileName));
    return Files.readAllBytes(file.toPath());
  }

  public String getContentType(String filename) {
    if (filename == null || filename.isEmpty()) {
      return "application/octet-stream";
    }

    int lastDot = filename.lastIndexOf('.');
    if (lastDot < 0 || lastDot == filename.length() - 1) {
      return "application/octet-stream";
    }

    String extension = filename.substring(lastDot + 1).toLowerCase();
    return MIME_TYPES.getOrDefault(extension, "application/octet-stream");
  }
}
