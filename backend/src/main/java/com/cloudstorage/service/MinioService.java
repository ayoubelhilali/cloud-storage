package com.cloudstorage.service;

import io.minio.*;
import java.io.InputStream;
import com.cloudstorage.util.ConfigLoader;

public class MinioService {
    private MinioClient minioClient;

    public MinioService() {
        minioClient = MinioClient.builder()
            .endpoint(ConfigLoader.getMinioEndpoint())
            .credentials(ConfigLoader.getMinioAccessKey(), ConfigLoader.getMinioSecretKey())
            .build();
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(ConfigLoader.getMinioBucketName()).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(ConfigLoader.getMinioBucketName()).build());
            }
        } catch (Exception e) {
            System.err.println("Error initializing MinIO: " + e.getMessage());
            // Do not throw RuntimeException here, allow the service to be instantiated
            // The connection test method will handle the status check
        }
    }

    public boolean testConnection() {
        try {
            minioClient.bucketExists(BucketExistsArgs.builder().bucket(ConfigLoader.getMinioBucketName()).build());
            return true;
        } catch (Exception e) {
            System.err.println("MinIO connection test failed: " + e.getMessage());
            return false;
        }
    }

    public String uploadFile(String key, InputStream inputStream, long size, String contentType) throws Exception {
        minioClient.putObject(PutObjectArgs.builder().bucket(ConfigLoader.getMinioBucketName()).object(key)
            .stream(inputStream, size, -1).contentType(contentType).build());
        return key;
    }

    public InputStream downloadFile(String key) throws Exception {
        return minioClient.getObject(GetObjectArgs.builder().bucket(ConfigLoader.getMinioBucketName()).object(key).build());
    }

    public void deleteFile(String key) throws Exception {
        minioClient.removeObject(RemoveObjectArgs.builder().bucket(ConfigLoader.getMinioBucketName()).object(key).build());
    }
}
