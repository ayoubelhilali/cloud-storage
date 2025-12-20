package com.cloudstorage.service;

import com.cloudstorage.database.ShareDAO;
import com.cloudstorage.model.FileMetadata;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class ShareService {

    private final ShareDAO shareDAO;
    private final MinioClient minioClient;
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    public ShareService(MinioClient minioClient) {
        this.shareDAO = new ShareDAO();
        this.minioClient = minioClient;
    }

    /**
     * Handles the logic of finding the user and creating the database record.
     */
    public String shareFile(String filename, long senderId, String recipientEmail) {
        // 1. Validate Email
        if (recipientEmail == null || !Pattern.compile(EMAIL_REGEX).matcher(recipientEmail).matches()) {
            return "Invalid email format.";
        }

        // 2. Find Recipient ID
        long recipientId = shareDAO.getUserIdByEmail(recipientEmail);

        // 3. Handle Guest User Creation (if user doesn't exist yet)
        if (recipientId == -1) {
            recipientId = shareDAO.createPlaceholderUser(recipientEmail);
            if (recipientId == -1) return "Error: Could not create guest account.";
        }

        // 4. Prevent Self-Sharing
        if (recipientId == senderId) return "You cannot share with yourself.";

        // 5. Find File ID
        // Uses the robust search (checks both system name and original name)
        long fileId = shareDAO.getFileIdByName(filename.trim(), senderId);
        if (fileId == -1) {
            return "Error: File '" + filename + "' not found.";
        }

        // 6. Execute Share in DB
        boolean success = shareDAO.shareFile(fileId, senderId, recipientId);
        return success ? "SUCCESS" : "File is already shared with this user.";
    }

    /**
     * Retrieves the list of files shared with a specific user.
     */
    public List<FileMetadata> getSharedFiles(long userId) {
        return shareDAO.getFilesSharedWithUser(userId);
    }

    /**
     * GENERATES THE DOWNLOAD LINK
     * This version adds 'response-content-disposition' to force the browser to download the file.
     */
    public String generateDownloadLink(long fileId, long requestingUserId) throws Exception {
        // 1. Security Check: Does the user have permission?
        if (!shareDAO.isSharedWith(fileId, requestingUserId)) {
            throw new SecurityException("Access Denied: File is not shared with you.");
        }

        // 2. Fetch File Metadata (Bucket, Key, Original Name)
        FileMetadata file = shareDAO.getFileById(fileId);
        if (file == null) {
            throw new Exception("File metadata not found in database.");
        }

        // 3. Determine the download filename
        // We prefer the 'Original Filename' so the user gets "Report.pdf" instead of "12345_Report.pdf"
        String downloadName = file.getOriginalFilename();
        if (downloadName == null || downloadName.isEmpty()) {
            downloadName = file.getFilename();
        }

        // 4. Force "Attachment" mode
        // This tells the browser: "Save this file to disk" instead of "Open this in a tab"
        Map<String, String> reqParams = new HashMap<>();
        reqParams.put("response-content-disposition", "attachment; filename=\"" + downloadName + "\"");

        // 5. Generate MinIO Presigned URL
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(file.getStorageBucket())
                        .object(file.getStorageKey())
                        .expiry(1, TimeUnit.HOURS) // Link is valid for 1 hour
                        .extraQueryParams(reqParams) // Applies the download force
                        .build());
    }
}