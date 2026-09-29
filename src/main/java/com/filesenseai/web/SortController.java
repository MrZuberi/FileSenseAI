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
        Path target = (path == null || path.isBlank())
                ? Paths.get(System.getProperty("user.home"))
                : Paths.get(path);

        if (!Files.isDirectory(target)) {
            target = Paths.get(System.getProperty("user.home"));
        }

        List<FolderListing.FolderEntry> subFolders = new ArrayList<>();
        int fileCount = 0;

        try (var paths = Files.list(target)) {
            for (Path entry : paths.sorted().toList()) {
                if (Files.isDirectory(entry)) {
                    subFolders.add(new FolderListing.FolderEntry(entry.getFileName().toString(), entry.toAbsolutePath().toString()));
                } else if (Files.isRegularFile(entry)) {
                    fileCount++;
                }
            }
        } catch (IOException ignored) {
        }

        Path parent = target.getParent();
        String parentPath = parent != null ? parent.toAbsolutePath().toString() : null;

        return new FolderListing(target.toAbsolutePath().toString(), parentPath, subFolders, fileCount);
    }

    @PostMapping("/jobs")
    public StartJobResponse startJob(@RequestBody StartJobRequest request) {
        Path folder = Paths.get(request.folderPath());
        String jobId = jobManager.startJob(folder, request.backup());
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