package com.filesenseai.web;

import com.filesenseai.job.JobManager;
import com.filesenseai.job.SortJob;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SortController {

    private final JobManager jobManager;

    public SortController(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @GetMapping("/browse")
    public FolderListing browse(@RequestParam(required = false) String path) {
        if (path == null || path.isBlank()) {
            return listDrives();
        }

        Path target = Paths.get(path);

        if (!Files.isDirectory(target)) {
            return listDrives();
        }

        List<FolderListing.FolderEntry> subFolders = new ArrayList<>();
        List<FolderListing.FileEntry> fileEntries = new ArrayList<>();

        try (var paths = Files.list(target)) {
            for (Path entry : paths.sorted().toList()) {
                if (Files.isDirectory(entry)) {
                    subFolders.add(new FolderListing.FolderEntry(entry.getFileName().toString(), entry.toAbsolutePath().toString()));
                } else if (Files.isRegularFile(entry)) {
                    fileEntries.add(buildFileEntry(entry));
                }
            }
        } catch (IOException ignored) {
        }

        return new FolderListing(target.toAbsolutePath().toString(), subFolders, fileEntries, false);
    }

    private FolderListing listDrives() {
        List<FolderListing.FolderEntry> drives = new ArrayList<>();

        for (File root : File.listRoots()) {
            drives.add(new FolderListing.FolderEntry(root.getPath(), root.getAbsolutePath()));
        }

        return new FolderListing("This PC", drives, List.of(), true);
    }

    private FolderListing.FileEntry buildFileEntry(Path file) {
        String name = file.getFileName().toString();
        String extension = "";
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = name.substring(dotIndex + 1).toLowerCase();
        }

        long sizeBytes;
        try {
            sizeBytes = Files.size(file);
        } catch (IOException exception) {
            sizeBytes = 0;
        }

        return new FolderListing.FileEntry(name, extension, formatSize(sizeBytes));
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.0f KB", bytes / 1024.0);
        }
        if (bytes < 1024L * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        }
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    @PostMapping("/jobs")
    public StartJobResponse startJob(@RequestBody StartJobRequest request) {
        Path folder = Paths.get(request.folderPath());
        String jobId = jobManager.startJob(folder);
        return new StartJobResponse(jobId);
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<JobStatusResponse> getJob(@PathVariable String jobId) {
        SortJob job = jobManager.getJob(jobId);

        if (job == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(JobStatusResponse.from(job));
    }
}