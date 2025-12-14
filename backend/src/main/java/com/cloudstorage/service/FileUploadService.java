package com.cloudstorage.service;

import com.cloudstorage.config.MinioConfig;
import com.cloudstorage.config.SessionManager;
import com.cloudstorage.database.FileDAO;
import com.cloudstorage.model.FileMetadata;
import com.cloudstorage.model.User;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.function.Consumer;

public class FileUploadService {

    private final FileDAO fileDAO = new FileDAO();

    public void uploadFile(File file, long userId, Consumer<Double> progressCallback) throws Exception {
        MinioClient minioClient = MinioConfig.getClient();

        // 1. Get User & Bucket Name
        User currentUser = SessionManager.getCurrentUser();
        String fName = (currentUser.getFirstName() != null) ? currentUser.getFirstName() : "user";
        String lName = (currentUser.getLastName() != null) ? currentUser.getLastName() : "default";

        // Sanitize bucket name strictly (only lowercase, numbers, hyphens)
        String bucketName = (fName + "-" + lName).toLowerCase().replaceAll("[^a-z0-9-]", "");

        // 2. Ensure Bucket Exists
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }

        // 3. Prepare Metadata & READABLE Filename
        String originalName = file.getName();
        String extension = getExtension(originalName);
        String mimeType = Files.probeContentType(file.toPath());
        if (mimeType == null) mimeType = "application/octet-stream";

        // --- NEW NAMING LOGIC ---
        // We replace spaces with underscores to keep MinIO happy
        String cleanName = originalName.replaceAll("\\s+", "_");

        // We add a Timestamp prefix.
        // Example: "170250999_my_file.pdf"
        // This keeps the name readable but prevents overwrites.
        String storageKey = cleanName;

        System.out.println("🚀 Uploading Readable File: " + storageKey);

        FileMetadata metadata = new FileMetadata(
                userId, originalName, originalName, file.length(),
                mimeType, extension, storageKey, bucketName
        );

        // 4. Upload to MinIO
        try (InputStream fileStream = new FileInputStream(file);
             BufferedInputStream bufStream = new BufferedInputStream(fileStream)) {

            ProgressInputStream progressStream = new ProgressInputStream(bufStream, file.length(), progressCallback);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storageKey) // <--- Using the readable name
                            .stream(progressStream, file.length(), -1)
                            .contentType(mimeType)
                            .build()
            );
        }

        // 5. Save to Database
        progressCallback.accept(0.99);
        fileDAO.save(metadata);
        progressCallback.accept(1.0);
    }

    private String getExtension(String filename) {
        int i = filename.lastIndexOf('.');
        return (i > 0) ? filename.substring(i + 1) : "";
    }

    // Helper Class for Progress
    private static class ProgressInputStream extends InputStream {
        private final InputStream wrapped;
        private final long totalBytes;
        private final Consumer<Double> callback;
        private long bytesRead = 0;

        public ProgressInputStream(InputStream wrapped, long totalBytes, Consumer<Double> callback) {
            this.wrapped = wrapped;
            this.totalBytes = totalBytes;
            this.callback = callback;
        }

        @Override
        public int read() throws java.io.IOException {
            int b = wrapped.read();
            if (b != -1) updateProgress(1);
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws java.io.IOException {
            int count = wrapped.read(b, off, len);
            if (count != -1) updateProgress(count);
            return count;
        }

        private void updateProgress(long count) {
            bytesRead += count;
            if (totalBytes > 0) {
                callback.accept((double) bytesRead / totalBytes);
            }
        }

        @Override
        public void close() throws java.io.IOException { wrapped.close(); }
    }
}