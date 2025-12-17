package com.cloudstorage.controller;

import com.cloudstorage.database.DatabaseConnection;
import com.cloudstorage.database.ShareDAO;
import com.cloudstorage.model.FileMetadata;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.regex.Pattern;

public class ShareController {

    private final ShareDAO shareDAO;
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    public ShareController() {
        this.shareDAO = new ShareDAO();
    }

    public String shareFileByName(String filename, long senderId, String recipientEmail) {
        // 1. Validate Email
        if (recipientEmail == null || !Pattern.compile(EMAIL_REGEX).matcher(recipientEmail).matches()) {
            return "Invalid email format.";
        }

        // 2. Find Recipient
        long recipientId = getUserIdByEmail(recipientEmail);
        if (recipientId == -1) {
            recipientId = createPlaceholderUser(recipientEmail);
            if (recipientId == -1) return "Error: Could not create guest account.";
        }

        if (recipientId == senderId) return "You cannot share with yourself.";

        // 3. FIND FILE ID (With Debugging)
        // We trim the filename to ensure no spaces cause issues
        String cleanFilename = filename.trim();
        long fileId = getFileIdByName(cleanFilename, senderId);

        // --- CRITICAL DEBUG CHANGE ---
        // If file is not found, we return the EXACT values we searched for so you can see them on screen.
        if (fileId == -1) {
            return "DEBUG: DB search failed. looking for File: [" + cleanFilename + "] with UserID: [" + senderId + "]";
        }

        // 4. Share
        boolean success = shareDAO.shareFile(fileId, senderId, recipientEmail);
        return success ? "SUCCESS" : "File is already shared with this user.";
    }

    public List<FileMetadata> getSharedFiles(long currentUserId) {
        return shareDAO.getFilesSharedWithUser(currentUserId);
    }

    // --- HELPERS ---

    private long getFileIdByName(String filename, long ownerId) {
        String sql = "SELECT id FROM files WHERE filename = ? AND user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, filename);
            stmt.setLong(2, ownerId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getLong("id");
        } catch (Exception e) { e.printStackTrace(); }
        return -1;
    }

    private long getUserIdByEmail(String email) {
        String sql = "SELECT id FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getLong("id");
        } catch (Exception e) { e.printStackTrace(); }
        return -1;
    }

    private long createPlaceholderUser(String email) {
        String sql = "INSERT INTO users (username, email, password_hash, first_name, last_name) VALUES (?, ?, ?, ?, ?)";
        String tempUsername = email.split("@")[0];
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, tempUsername);
            stmt.setString(2, email);
            stmt.setString(3, "temp1234");
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