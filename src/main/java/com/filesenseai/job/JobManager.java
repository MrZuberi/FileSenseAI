package com.filesenseai.job;

import com.filesenseai.pipeline.SortResult;
import com.filesenseai.pipeline.SortingPipeline;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class JobManager {

    private final SortingPipeline sortingPipeline;
    private final Map<String, SortJob> jobs = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public JobManager(SortingPipeline sortingPipeline) {
        this.sortingPipeline = sortingPipeline;
    }

    public String startJob(Path folder, boolean performBackup) {
        String jobId = UUID.randomUUID().toString();
        SortJob job = new SortJob();
        jobs.put(jobId, job);

        executorService.submit(() -> {
            try {
                SortResult result = sortingPipeline.run(folder, performBackup, (message, current, total) -> {
                    job.setMessage(message);
                    job.setCurrent(current);
                    job.setTotal(total);
                });
                job.setResult(result);
                job.setStatus(SortJob.Status.DONE);
            } catch (Exception exception) {
                job.setErrorMessage(exception.getMessage() != null ? exception.getMessage() : exception.getClass().getSimpleName());
                job.setStatus(SortJob.Status.ERROR);
            }
        });

        return jobId;
    }

    public SortJob getJob(String jobId) {
        return jobs.get(jobId);
    }
}