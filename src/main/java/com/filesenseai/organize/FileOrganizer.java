package com.filesenseai.organize;

import com.filesenseai.extraction.ExtractedDocument;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FileOrganizer {

    public OrganizePlan buildPlan(List<ExtractedDocument> documents, List<Integer> clusterLabels, Map<Integer, String> clusterNames) {
        Map<String, List<Path>> foldersToFiles = new LinkedHashMap<>();

        for (int i = 0; i < documents.size(); i++) {
            int cluster = clusterLabels.get(i);
            String folderName = clusterNames.getOrDefault(cluster, "Miscellaneous");
            foldersToFiles.computeIfAbsent(folderName, key -> new ArrayList<>()).add(documents.get(i).filePath());
        }

        return new OrganizePlan(foldersToFiles);
    }

    public void applyPlan(Path rootFolder, OrganizePlan plan) throws IOException {
        for (Map.Entry<String, List<Path>> entry : plan.foldersToFiles().entrySet()) {
            Path targetFolder = rootFolder.resolve(sanitizeForFileSystem(entry.getKey()));
            Files.createDirectories(targetFolder);

            for (Path file : entry.getValue()) {
                Path destination = resolveNameConflict(targetFolder.resolve(file.getFileName()));
                Files.move(file, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private Path resolveNameConflict(Path destination) {
        if (!Files.exists(destination)) {
            return destination;
        }

        String fileName = destination.getFileName().toString();
        String baseName = fileName;
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex > 0) {
            baseName = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex);
        }

        int counter = 1;
        Path candidate = destination;

        while (Files.exists(candidate)) {
            candidate = destination.getParent().resolve(baseName + "-" + counter + extension);
            counter++;
        }

        return candidate;
    }

    private String sanitizeForFileSystem(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "-").trim();
    }
}