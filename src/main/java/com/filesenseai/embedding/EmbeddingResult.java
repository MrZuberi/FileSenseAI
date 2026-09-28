package com.filesenseai.embedding;

import java.nio.file.Path;

public record EmbeddingResult(Path filePath, float[] vector) {
}