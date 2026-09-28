package com.filesenseai.organize;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record OrganizePlan(Map<String, List<Path>> foldersToFiles) {
}