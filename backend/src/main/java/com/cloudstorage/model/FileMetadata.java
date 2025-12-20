package com.cloudstorage.model;

public class FileMetadata {

    private long id;
    private long userId;
    private Long folderId; // nullable
    private String filename;
    private String originalFilename; // Added this field as it appeared in your DAO
    private long fileSize;
    private String mimeType;
    private String fileExtension;
    private String storageKey;
    private String storageBucket;
    private boolean isFavorite;

    // Constructor (used when uploading/fetching)
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
        this.folderId = null;
        this.isFavorite = false;
    }

    // Empty constructor
    public FileMetadata() {
    }

    // ---------- Getters & Setters ----------
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getStorageBucket() {
        return storageBucket;
    }

    public void setStorageBucket(String storageBucket) {
        this.storageBucket = storageBucket;
    }

    public Long getFolderId() {
        return folderId;
    }

    public void setFolderId(Long folderId) {
        this.folderId = folderId;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }
}