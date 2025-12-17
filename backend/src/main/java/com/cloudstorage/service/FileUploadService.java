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

        // 1. Get Bucket Name from User
        User currentUser = SessionManager.getCurrentUser();
        String fName = (currentUser != null) ? currentUser.getFirstName() : "user";
        String lName = (currentUser != null) ? currentUser.getLastName() : "default";
        String bucketName = (fName + "-" + lName).toLowerCase().replaceAll("[^a-z0-9-]", "");

        // 2. Ensure Bucket Exists
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }

        // 3. Prepare File Data
        String originalName = file.getName();
        String extension = getExtension(originalName);
        String mimeType = Files.probeContentType(file.toPath());
        if (mimeType == null) mimeType = "application/octet-stream";

        // FIX: Create a Unique Storage Key (Timestamp + Clean Name)
        String cleanName = originalName.replaceAll("\\s+", "_");
        String storageKey = System.currentTimeMillis() + "_" + cleanName;

        // 4. Upload to MinIO
        try (InputStream fileStream = new FileInputStream(file);
             BufferedInputStream bufStream = new BufferedInputStream(fileStream)) {

            // Upload logic...
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storageKey) // Use the Unique Key here
                            .stream(bufStream, file.length(), -1)
                            .contentType(mimeType)
                            .build()
            );
        }

        // 5. CRITICAL: Save to Database
        FileMetadata metadata = new FileMetadata(
                userId, originalName, originalName, file.length(),
                mimeType, extension, storageKey, bucketName
        );

        fileDAO.save(metadata); // <--- This fixes your "File not found" error!

        if (progressCallback != null) progressCallback.accept(1.0);
    }

    private String getExtension(String filename) {
        int i = filename.lastIndexOf('.');
        return (i > 0) ? filename.substring(i + 1) : "";
    }
}