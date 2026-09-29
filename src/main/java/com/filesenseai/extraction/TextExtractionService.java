package com.filesenseai.extraction;

import com.filesenseai.pipeline.ProgressListener;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Service
public class TextExtractionService {

    private static final int MAX_TEXT_LENGTH = 20000;
    private static final int MIN_MEANINGFUL_TEXT_LENGTH = 20;
    private static final int THREAD_COUNT = 6;

    public List<ExtractedDocument> extractFromFolder(Path folder, ProgressListener progressListener) throws IOException {
        List<Path> files;

        try (Stream<Path> paths = Files.list(folder)) {
            files = paths.filter(Files::isRegularFile).toList();
        }

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        AtomicInteger completed = new AtomicInteger(0);
        List<Future<ExtractedDocument>> futures = new ArrayList<>();

        for (Path file : files) {
            Callable<ExtractedDocument> task = () -> {
                ExtractedDocument document = buildDocument(file);
                int done = completed.incrementAndGet();
                progressListener.onProgress("Reading " + file.getFileName(), done, files.size());
                return document;
            };
            futures.add(executorService.submit(task));
        }

        List<ExtractedDocument> documents = new ArrayList<>();
        for (Future<ExtractedDocument> future : futures) {
            try {
                documents.add(future.get());
            } catch (Exception exception) {
                continue;
            }
        }

        executorService.shutdown();
        return documents;
    }

    private ExtractedDocument buildDocument(Path file) {
        return new ExtractedDocument(file, extractContent(file));
    }

    private String extractContent(Path file) {
        Metadata metadata = new Metadata();
        String parsedText = "";

        try {
            Tika tika = new Tika();
            parsedText = tika.parseToString(file.toFile(), metadata);
        } catch (Exception exception) {
            parsedText = "";
        }

        if (parsedText != null && parsedText.trim().length() >= MIN_MEANINGFUL_TEXT_LENGTH) {
            String trimmed = parsedText.trim();
            return trimmed.length() > MAX_TEXT_LENGTH ? trimmed.substring(0, MAX_TEXT_LENGTH) : trimmed;
        }

        return buildFallbackDescription(file, metadata);
    }

    private String buildFallbackDescription(Path file, Metadata metadata) {
        StringBuilder description = new StringBuilder();

        String fileName = file.getFileName().toString();
        String baseName = fileName;
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex + 1).toLowerCase();
        }

        description.append(baseName.replaceAll("[_\\-.]+", " ")).append(" ");
        description.append(categoryForExtension(extension)).append(" ");

        for (String name : metadata.names()) {
            String value = metadata.get(name);
            if (value != null && !value.isBlank() && isUsefulMetadataField(name)) {
                description.append(value).append(" ");
            }
        }

        return description.toString().trim();
    }

    private boolean isUsefulMetadataField(String name) {
        String lowerName = name.toLowerCase();
        return lowerName.contains("title") || lowerName.contains("author") || lowerName.contains("subject")
                || lowerName.contains("keywords") || lowerName.contains("description") || lowerName.contains("make")
                || lowerName.contains("model") || lowerName.contains("software") || lowerName.contains("creator")
                || lowerName.contains("date");
    }

    private String categoryForExtension(String extension) {
        return switch (extension) {
            case "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "tiff" -> "photo image picture";
            case "mp4", "mov", "avi", "mkv", "wmv" -> "video recording";
            case "mp3", "wav", "flac", "aac", "m4a" -> "audio music sound";
            case "zip", "rar", "7z", "tar", "gz" -> "archive compressed";
            case "exe", "msi", "dll" -> "program application";
            case "psd", "ai", "sketch", "fig" -> "design file";
            case "xlsx", "xls", "csv" -> "spreadsheet data";
            case "ppt", "pptx" -> "presentation slides";
            default -> extension.isBlank() ? "file" : extension + " file";
        };
    }
}