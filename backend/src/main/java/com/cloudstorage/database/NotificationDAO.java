package com.cloudstorage.database;

import com.cloudstorage.model.Notification;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    /**
     * Creates a new notification in the database
     */
    public static Long createNotification(Notification notification) {
        String sql = "INSERT INTO notifications (user_id, type, title, message, action_url, is_read, created_at, expires_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, notification.getUserId());
            stmt.setString(2, notification.getType());
            stmt.setString(3, notification.getTitle());
            stmt.setString(4, notification.getMessage());
            stmt.setString(5, notification.getActionUrl());
            stmt.setBoolean(6, notification.isRead());
            stmt.setTimestamp(7, Timestamp.valueOf(notification.getCreatedAt() != null ?
                notification.getCreatedAt() : LocalDateTime.now()));

            if (notification.getExpiresAt() != null) {
                stmt.setTimestamp(8, Timestamp.valueOf(notification.getExpiresAt()));
            } else {
                stmt.setNull(8, Types.TIMESTAMP);
            }

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Long id = rs.getLong(1);
                System.out.println("✅ Notification created with ID: " + id);
                return id;
            }
        } catch (SQLException e) {
            System.err.println("❌ CREATE NOTIFICATION FAILED: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets all notifications for a user (unread first, then recent)
     */
    public static List<Notification> getNotificationsByUserId(Long userId) {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? AND (expires_at IS NULL OR expires_at > NOW()) " +
                     "ORDER BY is_read ASC, created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                notifications.add(mapResultSetToNotification(rs));
            }

            System.out.println("✅ Retrieved " + notifications.size() + " notifications for user " + userId);
        } catch (SQLException e) {
            System.err.println("❌ GET NOTIFICATIONS FAILED: " + e.getMessage());
            e.printStackTrace();
        }
        return notifications;
    }

    /**
     * Gets unread notification count for a user
     */
    public static int getUnreadCount(Long userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE " +
                     "AND (expires_at IS NULL OR expires_at > NOW())";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ GET UNREAD COUNT FAILED: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Marks a notification as read
     */
    public static boolean markAsRead(Long notificationId, Long userId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ? AND user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, notificationId);
            stmt.setLong(2, userId);
            int affected = stmt.executeUpdate();

            if (affected > 0) {
                System.out.println("✅ Notification " + notificationId + " marked as read");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ MARK AS READ FAILED: " + e.getMessage());
        }
        return false;
    }

    /**
     * Marks all notifications as read for a user
     */
    public static boolean markAllAsRead(Long userId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            int affected = stmt.executeUpdate();

            System.out.println("✅ Marked " + affected + " notifications as read");
            return true;
        } catch (SQLException e) {
            System.err.println("❌ MARK ALL AS READ FAILED: " + e.getMessage());
        }
        return false;
    }

    /**
     * Deletes a notification
     */
    public static boolean deleteNotification(Long notificationId, Long userId) {
        String sql = "DELETE FROM notifications WHERE id = ? AND user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, notificationId);
            stmt.setLong(2, userId);
            int affected = stmt.executeUpdate();

            if (affected > 0) {
                System.out.println("✅ Notification " + notificationId + " deleted");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ DELETE NOTIFICATION FAILED: " + e.getMessage());
        }
        return false;
    }

    /**
     * Deletes all read notifications for a user
     */
    public static boolean deleteAllRead(Long userId) {
        String sql = "DELETE FROM notifications WHERE user_id = ? AND is_read = TRUE";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            int affected = stmt.executeUpdate();

            System.out.println("✅ Deleted " + affected + " read notifications");
            return true;
        } catch (SQLException e) {
            System.err.println("❌ DELETE ALL READ FAILED: " + e.getMessage());
        }
        return false;
    }

    /**
     * Helper method to map ResultSet to Notification object
     */
    private static Notification mapResultSetToNotification(ResultSet rs) throws SQLException {
        Notification notification = new Notification();
        notification.setId(rs.getLong("id"));
        notification.setUserId(rs.getLong("user_id"));
        notification.setType(rs.getString("type"));
        notification.setTitle(rs.getString("title"));
        notification.setMessage(rs.getString("message"));
        notification.setActionUrl(rs.getString("action_url"));
        notification.setRead(rs.getBoolean("is_read"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            notification.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp expiresAt = rs.getTimestamp("expires_at");
        if (expiresAt != null) {
            notification.setExpiresAt(expiresAt.toLocalDateTime());
        }

        return notification;
    }
}

