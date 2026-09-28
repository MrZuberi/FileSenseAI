package com.filesenseai.extraction;

import java.nio.file.Path;

public record ExtractedDocument(Path filePath, String text) {
}