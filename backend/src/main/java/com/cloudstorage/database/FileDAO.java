package com.cloudstorage.database;

import com.cloudstorage.model.FileMetadata;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileDAO {

    // 1. SAVE / UPLOAD
    public static long saveFileRecord(FileMetadata metadata) throws SQLException {
        String sql = "INSERT INTO files (user_id, folder_id, filename, file_size, mime_type, is_favorite, storage_key, storage_bucket, uploaded_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW()) RETURNING id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, metadata.getUserId());

            if (metadata.getFolderId() == null) {
                stmt.setNull(2, Types.BIGINT);
            } else {
                stmt.setLong(2, metadata.getFolderId());
            }

            stmt.setString(3, metadata.getFilename());
            stmt.setLong(4, metadata.getFileSize());
            stmt.setString(5, metadata.getMimeType());
            stmt.setBoolean(6, metadata.isFavorite());
            stmt.setString(7, metadata.getStorageKey());
            stmt.setString(8, metadata.getStorageBucket());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ DB SAVE FAILED: " + e.getMessage());
            throw e;
        }
        return -1;
    }

    // 2. DELETE
    public static void deleteFileRecord(String fileName, int userId) throws SQLException {
        String sql = "DELETE FROM files WHERE filename = ? AND user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, fileName);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        }
    }

    // 3. SET FAVORITE (This is the method causing your error - ensure it looks exactly like this)
    public static boolean setFavorite(long userId, String fileName, boolean isFavorite, String bucketName) {
        // A. Try to UPDATE existing record
        String updateSql = "UPDATE files SET is_favorite = ? WHERE user_id = ? AND filename = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {

            updateStmt.setBoolean(1, isFavorite);
            updateStmt.setLong(2, userId);
            updateStmt.setString(3, fileName);

            int affectedRows = updateStmt.executeUpdate();

            if (affectedRows > 0) return true;

            // B. If UPDATE failed, INSERT it (Upsert)
            String insertSql = "INSERT INTO files (user_id, filename, is_favorite, file_size, storage_key, storage_bucket, uploaded_at) VALUES (?, ?, ?, 0, ?, ?, NOW())";

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setLong(1, userId);
                insertStmt.setString(2, fileName);
                insertStmt.setBoolean(3, isFavorite);
                insertStmt.setString(4, fileName);
                insertStmt.setString(5, bucketName); // <--- Matches the argument passed from FileRowFactory

                insertStmt.executeUpdate();
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ SET FAVORITE FAILED: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // 4. FETCH ALL FAVORITES (Full Object)
    public static List<FileMetadata> getFavoriteFiles(long userId) throws SQLException {
        String sql = "SELECT * FROM files WHERE user_id = ? AND is_favorite = TRUE ORDER BY uploaded_at DESC";
        List<FileMetadata> favoriteFiles = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                FileMetadata metadata = new FileMetadata();
                metadata.setId(rs.getLong("id"));
                metadata.setUserId(rs.getLong("user_id"));
                metadata.setFolderId(rs.getObject("folder_id") != null ? rs.getLong("folder_id") : null);
                metadata.setFilename(rs.getString("filename"));
                metadata.setFileSize(rs.getLong("file_size"));
                metadata.setMimeType(rs.getString("mime_type"));
                metadata.setStorageKey(rs.getString("storage_key"));
                metadata.setStorageBucket(rs.getString("storage_bucket"));
                metadata.setFavorite(rs.getBoolean("is_favorite"));

                favoriteFiles.add(metadata);
            }
        }
        return favoriteFiles;
    }

    // 5. FETCH FAVORITE FILENAMES
    public static List<String> getFavoriteFilenames(long userId) {
        List<String> favorites = new ArrayList<>();
        String sql = "SELECT filename FROM files WHERE user_id = ? AND is_favorite = TRUE";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    favorites.add(rs.getString("filename"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return favorites;
    }

    // 6. DASHBOARD STATS: Total Storage Used
    public long getTotalStorageUsed(long userId) {
        String sql = "SELECT SUM(file_size) FROM files WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // 7. DASHBOARD STATS: Breakdown by Type
    public Map<String, Long> getStorageBreakdown(long userId) {
        Map<String, Long> breakdown = new HashMap<>();
        String sql = "SELECT mime_type, SUM(file_size) FROM files WHERE user_id = ? GROUP BY mime_type";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String type = rs.getString(1);
                long size = rs.getLong(2);
                String label = simplifyMimeType(type);
                breakdown.merge(label, size, Long::sum);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return breakdown;
    }

    private String simplifyMimeType(String mime) {
        if (mime == null) return "Other";
        if (mime.contains("image")) return "Images";
        if (mime.contains("video")) return "Videos";
        if (mime.contains("pdf") || mime.contains("word") || mime.contains("text")) return "Documents";
        return "Other";
    }
}