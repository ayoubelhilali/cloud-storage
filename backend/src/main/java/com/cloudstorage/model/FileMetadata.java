package com.cloudstorage.model;

public class FileMetadata {
    private long id;
    private long userId;
    private Long folderId; // Use wrapper class Long to allow null
    private String filename;
    private String originalFilename;
    private long fileSize;
    private String mimeType;
    private String fileExtension;
    private String storageKey;
    private String storageBucket;

    public FileMetadata(long userId, String filename, String originalFilename, long fileSize,
                        String mimeType, String fileExtension, String storageKey, String storageBucket) {
        this.userId = userId;
        this.filename = filename;
        this.originalFilename = originalFilename;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.fileExtension = fileExtension;
        this.storageKey = storageKey;
        this.storageBucket = storageBucket;
        this.folderId = null; // Default to root
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public Long getFolderId() { return folderId; }
    public void setFolderId(Long folderId) { this.folderId = folderId; }

    public String getFilename() { return filename; }
    public String getOriginalFilename() { return originalFilename; }
    public long getFileSize() { return fileSize; }
    public String getMimeType() { return mimeType; }
    public String getFileExtension() { return fileExtension; }
    public String getStorageKey() { return storageKey; }
    public String getStorageBucket() { return storageBucket; }
}