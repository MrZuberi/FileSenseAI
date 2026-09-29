package com.filesenseai.pipeline;

import com.filesenseai.clustering.ClusterNamer;
import com.filesenseai.clustering.ClusteringService;
import com.filesenseai.embedding.EmbeddingResult;
import com.filesenseai.embedding.EmbeddingService;
import com.filesenseai.extraction.ExtractedDocument;
import com.filesenseai.extraction.TextExtractionService;
import com.filesenseai.organize.FileOrganizer;
import com.filesenseai.organize.OrganizePlan;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SortingPipeline {

    private final TextExtractionService textExtractionService;
    private final EmbeddingService embeddingService;
    private final ClusteringService clusteringService;
    private final ClusterNamer clusterNamer;
    private final FileOrganizer fileOrganizer;

    public SortingPipeline(TextExtractionService textExtractionService,
                            EmbeddingService embeddingService,
                            ClusteringService clusteringService,
                            ClusterNamer clusterNamer,
                            FileOrganizer fileOrganizer) {
        this.textExtractionService = textExtractionService;
        this.embeddingService = embeddingService;
        this.clusteringService = clusteringService;
        this.clusterNamer = clusterNamer;
        this.fileOrganizer = fileOrganizer;
    }

    public SortResult run(Path folder, ProgressListener progressListener) throws IOException {
        progressListener.onProgress("Reading files from " + folder, 0, 0);
        List<ExtractedDocument> documents = textExtractionService.extractFromFolder(folder, progressListener);

        if (documents.isEmpty()) {
            throw new IllegalStateException("No files were found in this folder");
        }

        progressListener.onProgress("Generating embeddings for " + documents.size() + " files", 0, documents.size());
        List<EmbeddingResult> embeddings = embeddingService.embedDocuments(documents);

        int clusterCount = clusteringService.suggestClusterCount(documents.size());
        progressListener.onProgress("Grouping files into " + clusterCount + " topics", 0, 0);
        List<Integer> clusterLabels = clusteringService.assignClusters(embeddings, clusterCount);

        progressListener.onProgress("Naming topic folders", 0, clusterCount);
        Map<Integer, String> clusterNames = clusterNamer.nameClusters(documents, clusterLabels, progressListener);

        progressListener.onProgress("Moving files into their new folders", documents.size(), documents.size());
        OrganizePlan plan = fileOrganizer.buildPlan(documents, clusterLabels, clusterNames);
        fileOrganizer.applyPlan(folder, plan);

        Map<String, List<String>> summary = new LinkedHashMap<>();
        for (Map.Entry<String, List<Path>> entry : plan.foldersToFiles().entrySet()) {
            List<String> fileNames = new ArrayList<>();
            for (Path path : entry.getValue()) {
                fileNames.add(path.getFileName().toString());
            }
            summary.put(entry.getKey(), fileNames);
        }

        progressListener.onProgress("Done", documents.size(), documents.size());
        return new SortResult(summary);
    }
}