package com.cloudstorage.model;

public class FileMetadata {

    private long id;
    private long userId;
    private Long folderId; // nullable
    private String filename;
    private long fileSize;
    private String mimeType;
    private String fileExtension;
    private String storageKey;
    private String storageBucket;
    private boolean isFavorite; // ⭐ NEW

    // Constructor (used when uploading)
    public FileMetadata(long userId, String filename, String originalFilename, long fileSize,
                        String mimeType, String fileExtension, String storageKey, String storageBucket) {
        this.userId = userId;
        this.filename = filename;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.fileExtension = fileExtension;
        this.storageKey = storageKey;
        this.storageBucket = storageBucket;
        this.folderId = null;
        this.isFavorite = false; // default
    }

    // Empty constructor (used for DB fetch)
    public FileMetadata() {}

    // ---------- Getters & Setters ----------

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public Long getFolderId() { return folderId; }
    public void setFolderId(Long folderId) { this.folderId = folderId; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }

    public String getStorageBucket() { return storageBucket; }
    public void setStorageBucket(String storageBucket) { this.storageBucket = storageBucket; }

    // ⭐ Favorites
    public boolean isFavorite() { return isFavorite; }
    public void setIsFavorite(boolean isFavorite) { this.isFavorite = isFavorite; }
}
