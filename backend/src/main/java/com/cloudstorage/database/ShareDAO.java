package com.cloudstorage.database;

import com.cloudstorage.model.FileMetadata;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ShareDAO {

    // 1. Share a file (Insert Record)
    public boolean shareFile(long fileId, long senderId, long recipientId) {
        String checkShareSql = "SELECT 1 FROM file_shares WHERE file_id = ? AND shared_with_user_id = ?";
        String insertShareSql = "INSERT INTO file_shares (file_id, shared_by_user_id, shared_with_user_id) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check if already shared
            try (PreparedStatement stmt = conn.prepareStatement(checkShareSql)) {
                stmt.setLong(1, fileId);
                stmt.setLong(2, recipientId);
                if (stmt.executeQuery().next()) return false; // Already shared
            }

            // Insert new share
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

    // 2. Get files shared with a specific user
    // Inside ShareDAO.java

    public List<FileMetadata> getFilesSharedWithUser(long userId) {
        List<FileMetadata> sharedFiles = new ArrayList<>();
        String sql = "SELECT f.* FROM files f " +
                "JOIN file_shares fs ON f.id = fs.file_id " +
                "WHERE fs.shared_with_user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                // 1. Get Filename
                String filename = rs.getString("filename");
                String originalName = rs.getString("original_filename");
                if (originalName == null) originalName = filename;

                // 2. Calculate Extension manually (Since DB column is missing)
                String extension = "";
                int i = filename.lastIndexOf('.');
                if (i > 0) {
                    extension = filename.substring(i + 1);
                }

                // 3. Create Object
                FileMetadata metadata = new FileMetadata(
                        rs.getLong("user_id"),
                        filename,
                        originalName,
                        rs.getLong("file_size"),
                        rs.getString("mime_type"),
                        extension, // <-- Use the calculated variable, NOT rs.getString("file_extension")
                        rs.getString("storage_key"),
                        rs.getString("storage_bucket")
                );
                metadata.setId(rs.getLong("id"));
                sharedFiles.add(metadata);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sharedFiles;
    }

    // 3. Check if a user has access to a file (For Security)
    public boolean isSharedWith(long fileId, long userId) {
        String sql = "SELECT 1 FROM file_shares WHERE file_id = ? AND shared_with_user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, fileId);
            stmt.setLong(2, userId);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 4. Get File Metadata by ID (Needed for MinIO Bucket/Key)
    public FileMetadata getFileById(long fileId) {
        String sql = "SELECT * FROM files WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, fileId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                FileMetadata fm = new FileMetadata(
                        rs.getLong("user_id"),
                        rs.getString("filename"),
                        rs.getString("original_filename"),
                        rs.getLong("file_size"),
                        rs.getString("mime_type"),
                        rs.getString("file_extension"),
                        rs.getString("storage_key"),
                        rs.getString("storage_bucket")
                );
                fm.setId(rs.getLong("id"));
                return fm;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    // --- Helpers moved from Controller ---

    public long getUserIdByEmail(String email) {
        String sql = "SELECT id FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getLong("id");
        } catch (Exception e) { e.printStackTrace(); }
        return -1;
    }

    public long getFileIdByName(String name, long ownerId) {
        // UPDATED SQL: Checks if 'name' matches EITHER the system filename OR the original filename
        String sql = "SELECT id FROM files WHERE (filename = ? OR original_filename = ?) AND user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String searchName = name.trim();

            // Debugging: Print exactly what we are looking for
            System.out.println("DEBUG: Searching for file: [" + searchName + "] owned by UserID: " + ownerId);

            stmt.setString(1, searchName); // Check 'filename' column
            stmt.setString(2, searchName); // Check 'original_filename' column
            stmt.setLong(3, ownerId);
            System.out.println(searchName + ownerId);// Must belong to the sender

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                System.out.println("file id.           "+rs.getLong("id"));
                return rs.getLong("id");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1; // Still not found
    }
    public long createPlaceholderUser(String email) {
        String sql = "INSERT INTO users (username, email, password_hash, first_name, last_name) VALUES (?, ?, ?, ?, ?)";
        String tempUsername = email.split("@")[0];
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, tempUsername);
            stmt.setString(2, email);
            stmt.setString(3, "temp1234"); // Placeholder password
            stmt.setString(4, "Guest");
            stmt.setString(5, "User");
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) return generatedKeys.getLong(1);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return -1;
    }
}