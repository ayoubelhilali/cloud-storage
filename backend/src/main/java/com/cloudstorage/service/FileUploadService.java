package com.cloudstorage.service;

import com.cloudstorage.config.MinioConfig;
import com.cloudstorage.database.FileDAO;
import com.cloudstorage.model.FileMetadata;
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

    // Updated signature: Accept bucketName from Controller
    public void uploadFile(File file, long userId, String bucketName, Consumer<Double> progressCallback) throws Exception {
        MinioClient minioClient = MinioConfig.getClient();

        // 1. Ensure Bucket Exists
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }

        // 2. Prepare Metadata
        String originalName = file.getName();
        String mimeType = Files.probeContentType(file.toPath());
        if (mimeType == null) mimeType = "application/octet-stream";

        // Replace spaces with underscores for storage key
        String storageKey = originalName.replaceAll("\\s+", "_");

        // Create Metadata Object
        FileMetadata metadata = new FileMetadata();
        metadata.setUserId(userId);
        metadata.setFolderId(null); // Root folder
        metadata.setFilename(originalName);
        metadata.setFileSize(file.length());
        metadata.setMimeType(mimeType);
        metadata.setFavorite(false); // Default
        metadata.setStorageKey(storageKey);
        metadata.setStorageBucket(bucketName); // CRITICAL: Save bucket name

        // 3. Upload to MinIO (Physical File)
        try (InputStream fileStream = new FileInputStream(file);
             BufferedInputStream bufStream = new BufferedInputStream(fileStream)) {

            ProgressInputStream progressStream = new ProgressInputStream(bufStream, file.length(), progressCallback);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storageKey)
                            .stream(progressStream, file.length(), -1)
                            .contentType(mimeType)
                            .build()
            );
        } catch (Exception e) {
            e.printStackTrace();
            throw e; // Stop here if MinIO fails
        }

        // 4. Save to Database (Metadata)
        try {
            if (progressCallback != null) progressCallback.accept(0.99);

            // Use the static method we created in FileDAO
            FileDAO.saveFileRecord(metadata);

            if (progressCallback != null) progressCallback.accept(1.0);
        } catch (Exception e) {
            // Optional: If DB fails, you might want to delete the file from MinIO to keep them in sync
            // minioClient.removeObject(...)
            e.printStackTrace();
            throw e;
        }
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
            if (totalBytes > 0 && callback != null) {
                callback.accept((double) bytesRead / totalBytes);
            }
        }

        @Override
        public void close() throws java.io.IOException { wrapped.close(); }
    }
}