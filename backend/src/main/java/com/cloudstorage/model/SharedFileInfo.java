package com.cloudstorage.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Extended FileMetadata with sharing information.
 * Contains sender details and share date.
 */
public class SharedFileInfo extends FileMetadata {
    
    private String senderName;
    private String senderEmail;
    private LocalDateTime sharedDate;
    private Long senderId;
    
    // Formatter for display
    private static final DateTimeFormatter DISPLAY_FORMAT = 
        DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm");
    
    public SharedFileInfo() {
        super();
    }
    
    public SharedFileInfo(FileMetadata base) {
        this.setId(base.getId());
        this.setUserId(base.getUserId());
        this.setFolderId(base.getFolderId());
        this.setFilename(base.getFilename());
        this.setOriginalFilename(base.getOriginalFilename());
        this.setFileSize(base.getFileSize());
        this.setMimeType(base.getMimeType());
        this.setFileExtension(base.getFileExtension());
        this.setStorageKey(base.getStorageKey());
        this.setStorageBucket(base.getStorageBucket());
        this.setFavorite(base.isFavorite());
    }
    
    // --- Getters and Setters ---
    
    public String getSenderName() {
        return senderName;
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    
    public String getSenderEmail() {
        return senderEmail;
    }
    
    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }
    
    public LocalDateTime getSharedDate() {
        return sharedDate;
    }
    
    public void setSharedDate(LocalDateTime sharedDate) {
        this.sharedDate = sharedDate;
    }
    
    public Long getSenderId() {
        return senderId;
    }
    
    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }
    
    /**
     * Returns formatted share date for display
     * Example: "Dec 23, 2025 at 14:30"
     */
    public String getFormattedSharedDate() {
        if (sharedDate == null) return "Unknown date";
        return "Shared on " + sharedDate.format(DISPLAY_FORMAT);
    }
    
    /**
     * Returns formatted sender info
     * Example: "John Doe (john@example.com)"
     */
    public String getFormattedSenderInfo() {
        if (senderName == null || senderName.isEmpty()) {
            return senderEmail != null ? senderEmail : "Unknown sender";
        }
        return senderName + (senderEmail != null ? " (" + senderEmail + ")" : "");
    }
}
