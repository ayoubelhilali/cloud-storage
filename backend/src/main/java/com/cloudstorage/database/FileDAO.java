package com.cloudstorage.database;

import com.cloudstorage.model.FileMetadata;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class FileDAO {
    public void save(FileMetadata metadata) throws SQLException {
        String sql = "INSERT INTO files (user_id, file_name, file_extension, file_size, mime_type, storage_key) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, metadata.getUserId());
            stmt.setString(2, metadata.getFileName());
            stmt.setString(3, metadata.getFileExtension());
            stmt.setLong(4, metadata.getFileSize());
            stmt.setString(5, metadata.getMimeType());
            stmt.setString(6, metadata.getStorageKey());
            stmt.executeUpdate();
        }
    }
}
