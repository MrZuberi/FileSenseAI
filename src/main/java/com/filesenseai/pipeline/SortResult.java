package com.filesenseai.pipeline;

import java.util.List;
import java.util.Map;

public record SortResult(Map<String, List<String>> foldersToFiles, String backupLocation) {
}