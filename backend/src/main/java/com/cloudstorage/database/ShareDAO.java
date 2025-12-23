package com.cloudstorage.database;

import com.cloudstorage.model.FileMetadata;
import com.cloudstorage.model.SharedFileInfo;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // 2. Get files shared with a specific user (ENHANCED with sender info)
    public List<SharedFileInfo> getFilesSharedWithUserEnhanced(long userId) {
        List<SharedFileInfo> sharedFiles = new ArrayList<>();
        // Query with optional shared_at column handling
        String sql = "SELECT f.*, " +
                "u.first_name AS sender_first_name, " +
                "u.last_name AS sender_last_name, " +
                "u.email AS sender_email, " +
                "fs.shared_by_user_id AS sender_id " +
                "FROM files f " +
                "JOIN file_shares fs ON f.id = fs.file_id " +
                "JOIN users u ON fs.shared_by_user_id = u.id " +
                "WHERE fs.shared_with_user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                // Calculate Extension
                String filename = rs.getString("filename");
                String extension = "";
                int i = filename.lastIndexOf('.');
                if (i > 0) {
                    extension = filename.substring(i + 1);
                }

                // Create SharedFileInfo
                SharedFileInfo info = new SharedFileInfo();
                info.setId(rs.getLong("id"));
                info.setUserId(rs.getLong("user_id"));
                info.setFilename(filename);
                info.setOriginalFilename(rs.getString("original_filename"));
                info.setFileSize(rs.getLong("file_size"));
                info.setMimeType(rs.getString("mime_type"));
                info.setFileExtension(extension);
                info.setStorageKey(rs.getString("storage_key"));
                info.setStorageBucket(rs.getString("storage_bucket"));
                
                // Set sender info
                String senderFirstName = rs.getString("sender_first_name");
                String senderLastName = rs.getString("sender_last_name");
                info.setSenderName((senderFirstName != null ? senderFirstName : "") + 
                                   " " + (senderLastName != null ? senderLastName : ""));
                info.setSenderEmail(rs.getString("sender_email"));
                info.setSenderId(rs.getLong("sender_id"));
                
                // Try to get shared_at date (might not exist in all schemas)
                try {
                    Timestamp sharedAt = rs.getTimestamp("shared_at");
                    if (sharedAt != null) {
                        info.setSharedDate(sharedAt.toLocalDateTime());
                    }
                } catch (SQLException ex) {
                    // Column doesn't exist, use file upload date as fallback
                    try {
                        Timestamp uploadedAt = rs.getTimestamp("uploaded_at");
                        if (uploadedAt != null) {
                            info.setSharedDate(uploadedAt.toLocalDateTime());
                        }
                    } catch (SQLException ignored) {}
                }

                sharedFiles.add(info);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sharedFiles;
    }

    // 2b. Get files shared with a specific user (legacy - for backward compatibility)
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

    // =================================================================================
    // REMOVE SHARE
    // =================================================================================

    /**
     * Removes a file share from a specific user.
     * Used when user moves a shared file to their personal folder.
     */
    public static boolean removeShare(long fileId, long userId) {
        String sql = "DELETE FROM file_shares WHERE file_id = ? AND shared_with_user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, fileId);
            stmt.setLong(2, userId);
            
            int affected = stmt.executeUpdate();
            System.out.println("DEBUG: Removed share for file " + fileId + " from user " + userId + " - affected: " + affected);
            return affected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error removing share: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // =================================================================================
    // SHARED FOLDERS WITH COLLABORATORS
    // =================================================================================

    /**
     * Gets shared folders grouped by sender with collaborator information.
     * Returns a list of virtual "folders" based on who shared files with the user.
     */
    public static List<Map<String, Object>> getSharedFoldersWithCollaborators(long userId) {
        List<Map<String, Object>> sharedFolders = new ArrayList<>();
        
        // Group files by sender to create virtual "shared folders"
        // Note: avatar_url column might not exist, so we use COALESCE with empty string
        String sql = """
            SELECT 
                u.id AS sender_id,
                u.first_name,
                u.last_name,
                u.email,
                COUNT(DISTINCT fs.file_id) AS file_count
            FROM file_shares fs
            JOIN users u ON fs.shared_by_user_id = u.id
            WHERE fs.shared_with_user_id = ?
            GROUP BY u.id, u.first_name, u.last_name, u.email
            ORDER BY file_count DESC
            LIMIT 5
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> folder = new HashMap<>();
                String firstName = rs.getString("first_name");
                String lastName = rs.getString("last_name");
                
                // Create folder name from sender
                String folderName = (firstName != null ? firstName : "") + "'s Files";
                folder.put("name", folderName);
                folder.put("fileCount", rs.getInt("file_count"));
                folder.put("senderId", rs.getLong("sender_id"));
                
                // Add sender as first collaborator
                List<Map<String, String>> collaborators = new ArrayList<>();
                Map<String, String> senderCollab = new HashMap<>();
                senderCollab.put("firstName", firstName != null ? firstName : "");
                senderCollab.put("lastName", lastName != null ? lastName : "");
                senderCollab.put("email", rs.getString("email"));
                senderCollab.put("avatarUrl", ""); // Avatar URL not available in this schema
                collaborators.add(senderCollab);
                
                folder.put("collaborators", collaborators);
                sharedFolders.add(folder);
            }

            // Enhance with additional collaborators (other users who share from same sender)
            for (Map<String, Object> folder : sharedFolders) {
                addAdditionalCollaborators(conn, folder, userId);
            }

        } catch (SQLException e) {
            System.err.println("Error loading shared folders: " + e.getMessage());
            e.printStackTrace();
        }
        
        return sharedFolders;
    }

    /**
     * Adds additional collaborators (other recipients of same sender's files)
     */
    @SuppressWarnings("unchecked")
    private static void addAdditionalCollaborators(Connection conn, Map<String, Object> folder, long currentUserId) {
        Long senderId = (Long) folder.get("senderId");
        if (senderId == null) return;

        // Note: avatar_url column does not exist in this schema
        String sql = """
            SELECT DISTINCT u.first_name, u.last_name
            FROM file_shares fs1
            JOIN file_shares fs2 ON fs1.file_id = fs2.file_id
            JOIN users u ON fs2.shared_with_user_id = u.id
            WHERE fs1.shared_by_user_id = ?
            AND fs2.shared_with_user_id != ?
            LIMIT 3
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, senderId);
            stmt.setLong(2, currentUserId);
            ResultSet rs = stmt.executeQuery();

            List<Map<String, String>> collaborators = (List<Map<String, String>>) folder.get("collaborators");
            while (rs.next() && collaborators.size() < 4) {
                Map<String, String> collab = new HashMap<>();
                collab.put("firstName", rs.getString("first_name"));
                collab.put("lastName", rs.getString("last_name"));
                collab.put("avatarUrl", ""); // Avatar URL not available
                collaborators.add(collab);
            }
        } catch (SQLException ignored) {}
    }
}