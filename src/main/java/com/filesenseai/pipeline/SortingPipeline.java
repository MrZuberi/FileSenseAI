package com.filesenseai.pipeline;

import com.filesenseai.backup.S3BackupService;
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
import java.util.List;
import java.util.Map;

@Service
public class SortingPipeline {

    private final TextExtractionService textExtractionService;
    private final EmbeddingService embeddingService;
    private final ClusteringService clusteringService;
    private final ClusterNamer clusterNamer;
    private final FileOrganizer fileOrganizer;
    private final S3BackupService s3BackupService;

    public SortingPipeline(TextExtractionService textExtractionService,
                            EmbeddingService embeddingService,
                            ClusteringService clusteringService,
                            ClusterNamer clusterNamer,
                            FileOrganizer fileOrganizer,
                            S3BackupService s3BackupService) {
        this.textExtractionService = textExtractionService;
        this.embeddingService = embeddingService;
        this.clusteringService = clusteringService;
        this.clusterNamer = clusterNamer;
        this.fileOrganizer = fileOrganizer;
        this.s3BackupService = s3BackupService;
    }

    public SortResult run(Path folder, boolean performBackup, ProgressListener progressListener) throws IOException {
        progressListener.onStatus("Reading files from " + folder);
        List<ExtractedDocument> documents = textExtractionService.extractFromFolder(folder);

        if (documents.isEmpty()) {
            throw new IllegalStateException("No readable files were found in this folder");
        }

        String backupLocation = null;
        if (performBackup && s3BackupService.isConfigured()) {
            progressListener.onStatus("Backing up original files to AWS S3");
            backupLocation = s3BackupService.backupFolder(folder);
        }

        progressListener.onStatus("Generating embeddings for " + documents.size() + " files");
        List<EmbeddingResult> embeddings = embeddingService.embedDocuments(documents);

        int clusterCount = clusteringService.suggestClusterCount(documents.size());
        progressListener.onStatus("Grouping files into " + clusterCount + " topics");
        List<Integer> clusterLabels = clusteringService.assignClusters(embeddings, clusterCount);

        progressListener.onStatus("Naming topic folders");
        Map<Integer, String> clusterNames = clusterNamer.nameClusters(documents, clusterLabels);

        progressListener.onStatus("Moving files into their new folders");
        OrganizePlan plan = fileOrganizer.buildPlan(documents, clusterLabels, clusterNames);
        fileOrganizer.applyPlan(folder, plan);

        progressListener.onStatus("Done");
        return new SortResult(plan.foldersToFiles(), backupLocation);
    }
}