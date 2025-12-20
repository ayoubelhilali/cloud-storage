package com.cloudstorage.controller;

import com.cloudstorage.model.FileMetadata;
import com.cloudstorage.service.ShareService;
import io.minio.MinioClient;
import java.util.List;

public class ShareController {

    private final ShareService shareService;

    // CORRECTED: Accept MinioClient in constructor.
    // This allows ShareDialogController (and others) to pass the connection in.
    public ShareController(MinioClient minioClient) {
        this.shareService = new ShareService(minioClient);
    }

    // Called when the user clicks "Share" in the dialog
    public String shareFileByName(String filename, long senderId, String recipientEmail) {
        return shareService.shareFile(filename, senderId, recipientEmail);
    }

    // Called to populate the "Shared with Me" view
    public List<FileMetadata> getSharedFiles(long currentUserId) {
        return shareService.getSharedFiles(currentUserId);
    }

    // CORRECTED: Returns String so the UI can open the browser
    public String getDownloadLink(long fileId, long currentUserId) throws Exception {
        return shareService.generateDownloadLink(fileId, currentUserId);
    }
}