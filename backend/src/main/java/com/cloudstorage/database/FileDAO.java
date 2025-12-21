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

    // 3. SET FAVORITE (Fixed to handle file size properly)
    public static boolean setFavorite(long userId, String fileName, boolean isFavorite, String bucketName, long fileSize) {
        // A. Try to UPDATE existing record
        String updateSql = "UPDATE files SET is_favorite = ? WHERE user_id = ? AND filename = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {

            updateStmt.setBoolean(1, isFavorite);
            updateStmt.setLong(2, userId);
            updateStmt.setString(3, fileName);

            int affectedRows = updateStmt.executeUpdate();

            if (affectedRows > 0) return true;

            // B. If UPDATE failed, INSERT it with proper file size
            String insertSql = "INSERT INTO files (user_id, filename, is_favorite, file_size, storage_key, storage_bucket, uploaded_at) VALUES (?, ?, ?, ?, ?, ?, NOW())";

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setLong(1, userId);
                insertStmt.setString(2, fileName);
                insertStmt.setBoolean(3, isFavorite);
                insertStmt.setLong(4, fileSize); // Use actual file size
                insertStmt.setString(5, fileName);
                insertStmt.setString(6, bucketName);

                insertStmt.executeUpdate();
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ SET FAVORITE FAILED: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Overloaded method for backward compatibility (defaults to 0)
    public static boolean setFavorite(long userId, String fileName, boolean isFavorite, String bucketName) {
        return setFavorite(userId, fileName, isFavorite, bucketName, 0);
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

    public static boolean createFolder(String name, long userId) {
        String sql = "INSERT INTO folders (name, user_id) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setLong(2, userId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("❌ CREATE FOLDER FAILED: " + e.getMessage());
            return false;
        }
    }

    /**
     * Fetches all folders for a user and calculates the number of files inside each.
     */
    public static List<Map<String, Object>> getFoldersByUserId(long userId) {
        List<Map<String, Object>> folders = new ArrayList<>();
        // Join folders with files to get dynamic file counts
        String sql = "SELECT f.id, f.name, COUNT(fi.id) as file_count " +
                "FROM folders f " +
                "LEFT JOIN files fi ON f.id = fi.folder_id " +
                "WHERE f.user_id = ? " +
                "GROUP BY f.id, f.name " +
                "ORDER BY f.name ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> folderData = new HashMap<>();
                    folderData.put("id", rs.getLong("id"));
                    folderData.put("name", rs.getString("name"));
                    folderData.put("file_count", rs.getInt("file_count"));
                    folders.add(folderData);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ FETCH FOLDERS FAILED: " + e.getMessage());
        }
        return folders;
    }

    public static boolean updateFileFolder(long userId, String fileName, long folderId, String bucket, long size) {
        // 1. Try to UPDATE the existing record first
        String updateSql = "UPDATE files SET folder_id = ? WHERE user_id = ? AND filename = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSql)) {

            pstmt.setLong(1, folderId);
            pstmt.setLong(2, userId);
            pstmt.setString(3, fileName);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) return true; // Successfully updated existing record

            // 2. If NO rows were updated, the file isn't in SQL yet. INSERT it.
            String insertSql = "INSERT INTO files (user_id, folder_id, filename, file_size, storage_key, storage_bucket, uploaded_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, NOW())";

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setLong(1, userId);
                insertStmt.setLong(2, folderId);
                insertStmt.setString(3, fileName);
                insertStmt.setLong(4, size);
                insertStmt.setString(5, fileName); // Storage key matches filename
                insertStmt.setString(6, bucket);

                insertStmt.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ FOLDER MOVE FAILED: " + e.getMessage());
            return false;
        }
    }
}