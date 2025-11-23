package com.cloudstorage.service;

import com.cloudstorage.model.FileMetadata;
import com.cloudstorage.database.FileDAO;
import java.io.InputStream;
import java.util.UUID;

public class FileService {
    public void uploadFile(FileMetadata metadata, InputStream fileStream) throws Exception {
        MinioService minio = new MinioService();
        String key = metadata.getUserId() + "/" + UUID.randomUUID().toString() + metadata.getFileExtension();
        minio.uploadFile(key, fileStream, metadata.getFileSize(), metadata.getMimeType());
        metadata.setStorageKey(key);
        // Sauvegarde en DB
        new FileDAO().save(metadata);
    }
}
