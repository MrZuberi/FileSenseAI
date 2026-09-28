package com.filesenseai.pipeline;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record SortResult(Map<String, List<Path>> foldersToFiles, String backupLocation) {
}