package com.cloudstorage.util;

import com.cloudstorage.database.NotificationDAO;
import com.cloudstorage.model.Notification;

import java.time.LocalDateTime;

/**
 * Utility class for creating system notifications
 */
public class NotificationHelper {

    /**
     * Creates a success notification
     */
    public static void createSuccessNotification(Long userId, String title, String message) {
        Notification notification = new Notification(userId, "success", title, message);
        NotificationDAO.createNotification(notification);
    }

    /**
     * Creates an info notification
     */
    public static void createInfoNotification(Long userId, String title, String message) {
        Notification notification = new Notification(userId, "info", title, message);
        NotificationDAO.createNotification(notification);
    }

    /**
     * Creates a warning notification
     */
    public static void createWarningNotification(Long userId, String title, String message) {
        Notification notification = new Notification(userId, "warning", title, message);
        NotificationDAO.createNotification(notification);
    }

    /**
     * Creates a danger/error notification
     */
    public static void createDangerNotification(Long userId, String title, String message) {
        Notification notification = new Notification(userId, "danger", title, message);
        NotificationDAO.createNotification(notification);
    }

    /**
     * Creates a notification with action URL
     */
    public static void createNotificationWithAction(Long userId, String type, String title, String message, String actionUrl) {
        Notification notification = new Notification(userId, type, title, message, actionUrl);
        NotificationDAO.createNotification(notification);
    }

    /**
     * Creates a notification that expires
     */
    public static void createExpiringNotification(Long userId, String type, String title, String message, LocalDateTime expiresAt) {
        Notification notification = new Notification(userId, type, title, message);
        notification.setExpiresAt(expiresAt);
        NotificationDAO.createNotification(notification);
    }

    // Example notification creators for common events

    public static void notifyFileUploaded(Long userId, String fileName) {
        createSuccessNotification(userId, "Upload Complete", "\"" + fileName + "\" was uploaded successfully!");
    }

    public static void notifyFileShared(Long userId, String fileName, String sharedWith) {
        createInfoNotification(userId, "File Shared", "\"" + fileName + "\" was shared with " + sharedWith);
    }

    public static void notifyFileDeleted(Long userId, String fileName) {
        createInfoNotification(userId, "File Deleted", "\"" + fileName + "\" was deleted from your storage");
    }

    public static void notifyStorageLow(Long userId, int percentUsed) {
        createWarningNotification(userId, "Storage Warning", "You've used " + percentUsed + "% of your storage. Consider upgrading or removing files.");
    }

    public static void notifyStorageFull(Long userId) {
        createDangerNotification(userId, "Storage Full", "Your storage is full! Please delete some files or upgrade your plan.");
    }

    public static void notifyNewSharedFile(Long userId, String fileName, String sharedBy) {
        createInfoNotification(userId, "New Shared File", sharedBy + " shared \"" + fileName + "\" with you");
    }

    public static void notifyFolderCreated(Long userId, String folderName) {
        createSuccessNotification(userId, "Folder Created", "Folder \"" + folderName + "\" was created successfully");
    }

    public static void notifyPasswordChanged(Long userId) {
        createSuccessNotification(userId, "Password Changed", "Your password was changed successfully");
    }

    public static void notifyLoginFromNewDevice(Long userId, String device, String location) {
        createWarningNotification(userId, "New Login Detected", "Login from " + device + " in " + location);
    }
}

