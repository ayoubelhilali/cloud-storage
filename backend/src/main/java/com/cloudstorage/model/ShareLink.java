package com.cloudstorage.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime; // Using modern Java time

/**
 * Represents the share_links table in the database.
 */
@Entity
@Table(name = "share_links")
public class ShareLink implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    // We map the foreign key column directly.
    // We also define the relationship below.
    @Column(name = "file_id", nullable = false)
    private Integer fileId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "share_token", unique = true, nullable = false, length = 255)
    private String shareToken;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // Matches the 'timestamp' type

    @Column(name = "max_downloads")
    private Integer maxDownloads;

    @Column(name = "download_count", columnDefinition = "integer default 0")
    private Integer downloadCount = 0;

    @Column(name = "success_count", columnDefinition = "integer default 0")
    private Integer successCount = 0;

    @Column(name = "permission", length = 50)
    private String permission;

    // --- Relationships ---
    // This links the 'fileId' column to the File entity
    /*@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", referencedColumnName = "id", insertable = false, updatable = false)
    private File file;

    // This links the 'userId' column to the User entity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;*/

    // --- Constructors ---

    // JPA requires a no-argument constructor
    public ShareLink() {
    }

    // --- Getters and Setters ---
    // (You would generate all getters and setters here)

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getFileId() {
        return fileId;
    }

    public void setFileId(Integer fileId) {
        this.fileId = fileId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getShareToken() {
        return shareToken;
    }

    public void setShareToken(String shareToken) {
        this.shareToken = shareToken;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Integer getMaxDownloads() {
        return maxDownloads;
    }

    public void setMaxDownloads(Integer maxDownloads) {
        this.maxDownloads = maxDownloads;
    }

    public Integer getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Integer downloadCount) {
        this.downloadCount = downloadCount;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    /*public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }*/
}
