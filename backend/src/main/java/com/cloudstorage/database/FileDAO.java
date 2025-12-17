package com.cloudstorage.database;

import com.cloudstorage.model.FileMetadata;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class FileDAO {

    public void save(FileMetadata file) {
        String sql = "INSERT INTO files (filename, original_filename, file_size, content_type, file_extension, storage_key, storage_bucket, user_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, file.getFilename());       // Display Name
            stmt.setString(2, file.getOriginalFilename()); // Original Name
            stmt.setLong(3, file.getFileSize());
            stmt.setString(4, file.getMimeType());
            stmt.setString(5, file.getFileExtension());
            stmt.setString(6, file.getStorageKey());     // MinIO Object Name
            stmt.setString(7, file.getStorageBucket());
            stmt.setLong(8, file.getUserId());

            stmt.executeUpdate();
            System.out.println("✅ Database: Saved metadata for " + file.getFilename());

        } catch (SQLException e) {
            System.err.println("❌ Database Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}