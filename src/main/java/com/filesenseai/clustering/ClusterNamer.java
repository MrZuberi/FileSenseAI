package com.filesenseai.clustering;

import com.filesenseai.ai.CohereClient;
import com.filesenseai.extraction.ExtractedDocument;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ClusterNamer {

    private final CohereClient cohereClient;

    public ClusterNamer(CohereClient cohereClient) {
        this.cohereClient = cohereClient;
    }

    public Map<Integer, String> nameClusters(List<ExtractedDocument> documents, List<Integer> clusterLabels) {
        Map<Integer, List<ExtractedDocument>> grouped = groupByCluster(documents, clusterLabels);
        Map<Integer, String> names = new HashMap<>();

        for (Map.Entry<Integer, List<ExtractedDocument>> entry : grouped.entrySet()) {
            names.put(entry.getKey(), nameCluster(entry.getValue()));
        }

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

        return cohereClient.generateFolderName(prompt);
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }
}