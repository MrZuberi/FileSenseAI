package com.filesenseai.backup;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class S3BackupService {

    private final String bucketName;
    private final String region;

    public S3BackupService(String bucketName, String region) {
        this.bucketName = bucketName;
        this.region = region;
    }

    public boolean isConfigured() {
        return bucketName != null && !bucketName.isBlank();
    }

    public String backupFolder(Path folder) throws IOException {
        Path zipFile = Files.createTempFile("filesenseai-backup-", ".zip");
        zipFolder(folder, zipFile);

        String objectKey = "filesenseai-backups/" + folder.getFileName() + "-" + System.currentTimeMillis() + ".zip";

        try (S3Client s3Client = S3Client.builder().region(Region.of(region)).build()) {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            s3Client.putObject(request, RequestBody.fromFile(zipFile));
        } finally {
            Files.deleteIfExists(zipFile);
        }

        return objectKey;
    }

    private void zipFolder(Path folder, Path zipFile) throws IOException {
        try (OutputStream fileOutputStream = Files.newOutputStream(zipFile);
             ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {

            try (var paths = Files.list(folder)) {
                for (Path file : paths.filter(Files::isRegularFile).toList()) {
                    zipOutputStream.putNextEntry(new ZipEntry(file.getFileName().toString()));
                    Files.copy(file, zipOutputStream);
                    zipOutputStream.closeEntry();
                }
            }
        }
    }
}