package com.filesenseai.extraction;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class TextExtractionService {

    private static final int MAX_TEXT_LENGTH = 20000;

    private final Tika tika = new Tika();

    public List<ExtractedDocument> extractFromFolder(Path folder) throws IOException {
        List<ExtractedDocument> documents = new ArrayList<>();

        try (Stream<Path> paths = Files.list(folder)) {
            List<Path> files = paths.filter(Files::isRegularFile).toList();

            for (Path file : files) {
                String text = extractSafely(file);
                if (text != null && !text.isBlank()) {
                    documents.add(new ExtractedDocument(file, text));
                }
            }
        }

        return documents;
    }

    private String extractSafely(Path file) {
        try {
            String content = tika.parseToString(file);

            if (content.length() > MAX_TEXT_LENGTH) {
                content = content.substring(0, MAX_TEXT_LENGTH);
            }

            return content;
        } catch (Exception exception) {
            return null;
        }
    }
}