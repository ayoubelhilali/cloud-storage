package com.cloudstorage.database;

import com.cloudstorage.model.FileMetadata;
import java.sql.*;

public class FileDAO {

    public long save(FileMetadata metadata) throws SQLException {
        // Ensure this SQL matches your actual database table columns exactly
        String sql = "INSERT INTO files " +
                "(user_id, folder_id, filename, original_filename, file_size, mime_type, file_extension, storage_key, storage_bucket) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, metadata.getUserId());

            // Handle Null Folder ID
            if (metadata.getFolderId() == null) {
                stmt.setNull(2, Types.BIGINT);
            } else {
                stmt.setLong(2, metadata.getFolderId());
            }

            stmt.setString(3, metadata.getFilename());
            stmt.setString(4, metadata.getOriginalFilename());
            stmt.setLong(5, metadata.getFileSize());
            stmt.setString(6, metadata.getMimeType());
            stmt.setString(7, metadata.getFileExtension());
            stmt.setString(8, metadata.getStorageKey());
            stmt.setString(9, metadata.getStorageBucket());

            System.out.println("Executing DB Insert for: " + metadata.getFilename());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                long newId = rs.getLong(1);
                System.out.println("DB Insert Successful. New File ID: " + newId);
                return newId;
            }
        } catch (SQLException e) {
            System.err.println("❌ DB SAVE FAILED: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw so the UI knows it failed
        }
        return -1;
    }
}