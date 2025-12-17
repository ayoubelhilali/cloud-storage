package com.cloudstorage.database;

import com.cloudstorage.model.FileMetadata;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ShareDAO {

    public boolean shareFile(long fileId, long senderId, String recipientEmail) {
        String findUserSql = "SELECT id FROM users WHERE email = ?";
        String checkShareSql = "SELECT 1 FROM file_shares WHERE file_id = ? AND shared_with_user_id = ?";
        String insertShareSql = "INSERT INTO file_shares (file_id, shared_by_user_id, shared_with_user_id) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            // 1. Find Recipient ID
            long recipientId = -1;
            try (PreparedStatement stmt = conn.prepareStatement(findUserSql)) {
                stmt.setString(1, recipientEmail);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    recipientId = rs.getLong("id");
                } else {
                    return false; // User not found
                }
            }

            // 2. Prevent self-sharing
            if (recipientId == senderId) return false;

            // 3. Check if already shared
            try (PreparedStatement stmt = conn.prepareStatement(checkShareSql)) {
                stmt.setLong(1, fileId);
                stmt.setLong(2, recipientId);
                if (stmt.executeQuery().next()) return false; // Already shared
            }

            // 4. Insert Share
            try (PreparedStatement stmt = conn.prepareStatement(insertShareSql)) {
                stmt.setLong(1, fileId);
                stmt.setLong(2, senderId);
                stmt.setLong(3, recipientId);
                stmt.executeUpdate();
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<FileMetadata> getFilesSharedWithUser(long userId) {
        List<FileMetadata> sharedFiles = new ArrayList<>();
        String sql = "SELECT f.*, u.username as owner_name FROM files f " +
                "JOIN file_shares fs ON f.id = fs.file_id " +
                "JOIN users u ON fs.shared_by_user_id = u.id " +
                "WHERE fs.shared_with_user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                FileMetadata metadata = new FileMetadata(
                        rs.getLong("user_id"),
                        rs.getString("filename"),
                        rs.getString("original_filename"),
                        rs.getLong("file_size"),
                        rs.getString("mime_type"),
                        rs.getString("file_extension"),
                        rs.getString("storage_key"),
                        rs.getString("storage_bucket")
                );
                metadata.setId(rs.getLong("id"));
                // Optional: You could store 'owner_name' in metadata if you extended the class
                sharedFiles.add(metadata);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sharedFiles;
    }
}