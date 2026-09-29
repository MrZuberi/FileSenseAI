package com.filesenseai.extraction;

import com.filesenseai.pipeline.ProgressListener;
import org.apache.tika.Tika;
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
                String text = extractSafely(file);
                int done = completed.incrementAndGet();
                progressListener.onProgress("Reading " + file.getFileName(), done, files.size());
                return (text != null && !text.isBlank()) ? new ExtractedDocument(file, text) : null;
            };
            futures.add(executorService.submit(task));
        }

        List<ExtractedDocument> documents = new ArrayList<>();
        for (Future<ExtractedDocument> future : futures) {
            try {
                ExtractedDocument document = future.get();
                if (document != null) {
                    documents.add(document);
                }
            } catch (Exception exception) {
                continue;
            }
        }

        executorService.shutdown();
        return documents;
    }

    private String extractSafely(Path file) {
        try {
            Tika tika = new Tika();
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