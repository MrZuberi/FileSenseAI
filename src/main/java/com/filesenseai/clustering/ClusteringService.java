package com.filesenseai.clustering;

import com.filesenseai.embedding.EmbeddingResult;
import org.springframework.stereotype.Service;
import smile.clustering.KMeans;

import java.util.ArrayList;
import java.util.List;

@Service
public class ClusteringService {

    public List<Integer> assignClusters(List<EmbeddingResult> embeddings, int clusterCount) {
        double[][] data = new double[embeddings.size()][];

        for (int i = 0; i < embeddings.size(); i++) {
            float[] vector = embeddings.get(i).vector();
            double[] row = new double[vector.length];
            for (int j = 0; j < vector.length; j++) {
                row[j] = vector[j];
            }
            data[i] = row;
        }

        KMeans result = KMeans.fit(data, clusterCount, 100, 1E-4);

        List<Integer> labels = new ArrayList<>();
        for (int label : result.y) {
            labels.add(label);
        }

        return labels;
    }

    public int suggestClusterCount(int fileCount) {
        int suggested = (int) Math.round(Math.sqrt(fileCount / 2.0));
        return Math.max(2, Math.min(10, suggested));
    }
}