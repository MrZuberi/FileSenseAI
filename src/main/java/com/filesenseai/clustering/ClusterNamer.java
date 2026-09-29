package com.filesenseai.clustering;

import com.filesenseai.ai.CohereClient;
import com.filesenseai.extraction.ExtractedDocument;
import com.filesenseai.pipeline.ProgressListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ClusterNamer {

    private static final int MAX_CONCURRENT_NAMING_CALLS = 4;

    private final CohereClient cohereClient;

    public ClusterNamer(CohereClient cohereClient) {
        this.cohereClient = cohereClient;
    }

    public Map<Integer, String> nameClusters(List<ExtractedDocument> documents, List<Integer> clusterLabels, ProgressListener progressListener) {
        Map<Integer, List<ExtractedDocument>> grouped = groupByCluster(documents, clusterLabels);
        Map<Integer, String> names = new ConcurrentHashMap<>();

        ExecutorService executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_NAMING_CALLS);
        AtomicInteger completed = new AtomicInteger(0);
        int totalClusters = grouped.size();
        List<Future<?>> futures = new ArrayList<>();

        for (Map.Entry<Integer, List<ExtractedDocument>> entry : grouped.entrySet()) {
            futures.add(executorService.submit(() -> {
                String name = nameCluster(entry.getValue());
                names.put(entry.getKey(), name);
                int done = completed.incrementAndGet();
                progressListener.onProgress("Named folder: " + name, done, totalClusters);
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception exception) {
                continue;
            }
        }

        executorService.shutdown();
        return names;
    }

    private Map<Integer, List<ExtractedDocument>> groupByCluster(List<ExtractedDocument> documents, List<Integer> clusterLabels) {
        Map<Integer, List<ExtractedDocument>> grouped = new HashMap<>();

        for (int i = 0; i < documents.size(); i++) {
            int cluster = clusterLabels.get(i);
            grouped.computeIfAbsent(cluster, key -> new ArrayList<>()).add(documents.get(i));
        }

        return grouped;
    }

    private String nameCluster(List<ExtractedDocument> clusterDocuments) {
        String fileNames = clusterDocuments.stream()
                .map(document -> document.filePath().getFileName().toString())
                .limit(10)
                .collect(Collectors.joining(", "));

        String sampleContent = clusterDocuments.stream()
                .map(document -> truncate(document.text(), 300))
                .limit(3)
                .collect(Collectors.joining("\n---\n"));

        String prompt = "These files belong to the same topic group. File names: " + fileNames
                + ". Sample content: " + sampleContent
                + ". Reply with only a short folder name of two to four words that describes this topic, no punctuation, no explanation.";

        try {
            return cohereClient.generateFolderName(prompt);
        } catch (Exception exception) {
            return fallbackName(clusterDocuments);
        }
    }

    private String fallbackName(List<ExtractedDocument> clusterDocuments) {
        Map<String, Integer> wordCounts = new HashMap<>();

        for (ExtractedDocument document : clusterDocuments) {
            String baseName = document.filePath().getFileName().toString();
            int dotIndex = baseName.lastIndexOf('.');
            if (dotIndex > 0) {
                baseName = baseName.substring(0, dotIndex);
            }

            for (String word : baseName.split("[^a-zA-Z]+")) {
                if (word.length() > 2) {
                    wordCounts.merge(word.toLowerCase(), 1, Integer::sum);
                }
            }
        }

        String mostCommon = wordCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (mostCommon == null) {
            return "Miscellaneous";
        }

        String capitalized = mostCommon.substring(0, 1).toUpperCase() + mostCommon.substring(1);
        return capitalized + " Files";
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }
}