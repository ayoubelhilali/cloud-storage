package com.cloudstorage.service;

import com.cloudstorage.database.FileDAO;
import io.minio.*;
import io.minio.messages.Bucket;
import io.minio.messages.Item;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileService {

    private final MinioClient minioClient;

    public FileService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public List<Map<String, String>> getUserFiles(String bucketName) {
        List<Map<String, String>> fileList = new ArrayList<>();

        try {
            // TEST 1: List ALL buckets to verify connection
            List<Bucket> buckets = minioClient.listBuckets();
            boolean bucketFoundInList = false;
            for (Bucket b : buckets) {
                if (b.name().equals(bucketName)) {
                    bucketFoundInList = true;
                }
            }
            if (!bucketFoundInList) {
                return fileList; // Return empty
            }

            // TEST 2: List Objects in the specific bucket
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .recursive(true) // Look inside folders
                            .build());

            for (Result<Item> result : results) {
                Item item = result.get();

                Map<String, String> fileData = new HashMap<>();
                fileData.put("name", item.objectName());
                fileData.put("size", String.valueOf(item.size()));
                fileData.put("lastModified", (item.lastModified() != null) ? item.lastModified().toString() : "");
                fileList.add(fileData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return fileList;
    }

    public InputStream getFileStream(String bucketName, String objectName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build());
    }

    public void deleteFile(String bucketName, String objectName, long userId) throws Exception {
        // 1. Delete from MinIO
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );

        // 2. Delete from Database
        // Assuming you have a static method or instance method in FileDAO
        FileDAO.deleteFileRecord(objectName, (int) userId);
    }
}