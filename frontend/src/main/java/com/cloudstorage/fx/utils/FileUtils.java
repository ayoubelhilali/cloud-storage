package com.cloudstorage.fx.utils;

import com.cloudstorage.config.MinioConfig;
import com.cloudstorage.config.SessionManager;
import com.cloudstorage.model.User;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class FileUtils {

    public static String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) return fileName.substring(i + 1).toLowerCase();
        return "file";
    }
    public static String formatSize(String sizeStr) {
        try {
            long bytes = Long.parseLong(sizeStr);
            if (bytes <= 0) return "0 B";
            final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
            int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
            return new DecimalFormat("#,##0.#").format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
        } catch (NumberFormatException e) {
            return "0 B";
        }
    }
    public static Paint getUserAvatarFill(String objectKey) {
        Image image;

        try {
            // Attempt to load the default image
            InputStream is = FileUtils.class.getResourceAsStream("/images/default-profile.jpg");

            if (is == null) {
                // This prevents the NullPointerException you're seeing
                System.err.println("CRITICAL: Default avatar not found at /images/default-avatar.png");
                // Return a simple colored fill so the app doesn't crash
                return Color.DODGERBLUE;
            }

            image = new Image(is);

            // If the user has a custom avatar, try to fetch it from MinIO
            if (objectKey != null && !objectKey.isEmpty()) {
                // ... (your MinIO presigned URL logic here) ...
                // String url = minioClient.getPresignedObjectUrl(...);
                // image = new Image(url, true); // true = background load
            }

        } catch (Exception e) {
            System.err.println("Error loading avatar: " + e.getMessage());
            return Color.DODGERBLUE;
        }

        return new ImagePattern(image);
    }
    public static double parseSizeToMB(String sizeStr) {
        try {
            long bytes = Long.parseLong(sizeStr);
            return bytes / (1024.0 * 1024.0);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    public static double sizeLeftPercent(double usedMB) {
        if (usedMB <= 0) return 100.0;

        final double maxMB = 5 * 1024.0; // 10 GB

        double leftMB = Math.max(0, maxMB - usedMB);
        return (leftMB / maxMB) * 100.0;
    }

    public static String truncateFileName(String fileName, int maxLength) {
        if (fileName == null) return "";
        if (fileName.length() <= maxLength) return fileName;

        // 1. Find the extension
        int lastDotIndex = fileName.lastIndexOf('.');

        // Handle files without extension (e.g. "Makefile") or hidden files (e.g. ".gitignore")
        if (lastDotIndex <= 0) {
            return fileName.substring(0, maxLength) + "...";
        }

        String extension = fileName.substring(lastDotIndex);
        String namePart = fileName.substring(0, lastDotIndex);

        // 2. Calculate how much space is left for the name
        int spaceForName = maxLength - 3 - extension.length();
        // Safety check: if extension is huge or limit is tiny, just standard truncate
        if (spaceForName < 1) {
            return fileName.substring(0, maxLength) + "...";
        }
        // 3. Rebuild: ShortName + ... + Extension
        return namePart.substring(0, spaceForName) + "..." + extension;
    }


    public static boolean isImage(String ext) {
        return Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp").contains(ext.toLowerCase());
    }

    public static boolean isVideo(String ext) {
        return Arrays.asList("mp4", "avi", "mov", "mkv", "flv", "wmv").contains(ext.toLowerCase());
    }

    public static boolean isAudio(String ext) {
        return Arrays.asList("mp3", "wav", "aac", "ogg", "flac").contains(ext.toLowerCase());
    }

    public static boolean isDocument(String ext) {
        return Arrays.asList("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt").contains(ext.toLowerCase());
    }
}