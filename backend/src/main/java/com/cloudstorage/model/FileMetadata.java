package com.cloudstorage.model;

public class FileMetadata {
    private String userId;
    private String fileName;
    private String fileExtension;
    private long fileSize;
    private String mimeType;
    private String storageKey;

    // Constructeurs, getters et setters
    public FileMetadata() {}

    public FileMetadata(String userId, String fileName, String fileExtension, long fileSize, String mimeType) {
        this.userId = userId;
        this.fileName = fileName;
        this.fileExtension = fileExtension;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileExtension() { return fileExtension; }
    public void setFileExtension(String fileExtension) { this.fileExtension = fileExtension; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
}
