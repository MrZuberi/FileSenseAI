package com.filesenseai.embedding;

import com.filesenseai.ai.CohereClient;
import com.filesenseai.extraction.ExtractedDocument;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmbeddingService {

    private final CohereClient cohereClient;
    private final DocumentSplitter documentSplitter = DocumentSplitters.recursive(800, 100);

    public EmbeddingService(CohereClient cohereClient) {
        this.cohereClient = cohereClient;
    }

    public List<EmbeddingResult> embedDocuments(List<ExtractedDocument> documents) {
        List<String> representativeTexts = new ArrayList<>();

        for (ExtractedDocument document : documents) {
            representativeTexts.add(buildRepresentativeText(document.text()));
        }

        List<float[]> vectors = cohereClient.embed(representativeTexts, "clustering");

        List<EmbeddingResult> results = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            results.add(new EmbeddingResult(documents.get(i).filePath(), vectors.get(i)));
        }

        return results;
    }

    private String buildRepresentativeText(String fullText) {
        Document document = Document.from(fullText);
        List<TextSegment> segments = documentSplitter.split(document);

        if (segments.isEmpty()) {
            return fullText;
        }

        if (segments.size() == 1) {
            return segments.get(0).text();
        }

        StringBuilder combined = new StringBuilder();
        combined.append(segments.get(0).text());
        combined.append(" ");
        combined.append(segments.get(segments.size() / 2).text());

        if (segments.size() > 2) {
            combined.append(" ");
            combined.append(segments.get(segments.size() - 1).text());
        }

        return combined.toString();
    }
}