package com.cloudstorage.fx.components;

import com.cloudstorage.fx.utils.FileUtils;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Service réutilisable pour la gestion des prévisualisations de médias.
 * Détecte automatiquement le type de fichier et génère les aperçus appropriés.
 */
public class MediaPreviewService {

    private final MinioClient minioClient;
    private final String bucketName;

    /**
     * Types de médias supportés
     */
    public enum MediaType {
        IMAGE,
        VIDEO,
        AUDIO,
        PDF,
        DOCUMENT,
        ARCHIVE,
        UNKNOWN
    }

    public MediaPreviewService(MinioClient minioClient, String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    /**
     * Détecte le type de média basé sur l'extension du fichier
     */
    public static MediaType detectMediaType(String filename) {
        if (filename == null || filename.isEmpty()) {
            return MediaType.UNKNOWN;
        }

        String ext = FileUtils.getFileExtension(filename).toLowerCase();

        if (FileUtils.isImage(ext)) {
            return MediaType.IMAGE;
        } else if (FileUtils.isVideo(ext)) {
            return MediaType.VIDEO;
        } else if (FileUtils.isAudio(ext)) {
            return MediaType.AUDIO;
        } else if (ext.equals("pdf")) {
            return MediaType.PDF;
        } else if (FileUtils.isDocument(ext)) {
            return MediaType.DOCUMENT;
        } else if (isArchive(ext)) {
            return MediaType.ARCHIVE;
        }

        return MediaType.UNKNOWN;
    }

    /**
     * Vérifie si l'extension correspond à une archive
     */
    private static boolean isArchive(String ext) {
        return ext.equals("zip") || ext.equals("rar") || ext.equals("7z") || ext.equals("tar") || ext.equals("gz");
    }

    /**
     * Génère une URL signée pour accéder au fichier
     */
    public String getPresignedUrl(String objectName, int expiryHours) throws Exception {
        if (minioClient == null) {
            throw new IllegalStateException("MinioClient non initialisé");
        }

        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(expiryHours, TimeUnit.HOURS)
                        .build()
        );
    }

    /**
     * Charge une image depuis MinIO
     */
    public Task<Image> loadImageAsync(String objectName) {
        return new Task<>() {
            @Override
            protected Image call() throws Exception {
                String url = getPresignedUrl(objectName, 1);
                return new Image(url, true);
            }
        };
    }

    /**
     * Génère une miniature pour une vidéo
     */
    public Task<Image> generateVideoThumbnailAsync(String objectName) {
        return new Task<>() {
            @Override
            protected Image call() throws Exception {
                try (InputStream stream = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .build())) {

                    try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(stream)) {
                        grabber.start();
                        Frame frame = grabber.grabImage();

                        if (frame != null) {
                            Java2DFrameConverter converter = new Java2DFrameConverter();
                            BufferedImage bufferedImage = converter.convert(frame);
                            grabber.stop();
                            return SwingFXUtils.toFXImage(bufferedImage, null);
                        } else {
                            grabber.stop();
                            throw new Exception("Impossible d'extraire une frame de la vidéo");
                        }
                    }
                }
            }
        };
    }

    /**
     * Génère un aperçu de la première page d'un PDF
     */
    public Task<Image> generatePdfPreviewAsync(String objectName) {
        return new Task<>() {
            @Override
            protected Image call() throws Exception {
                try (InputStream stream = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .build())) {

                    try (PDDocument document = PDDocument.load(stream)) {
                        PDFRenderer renderer = new PDFRenderer(document);
                        BufferedImage bufferedImage = renderer.renderImageWithDPI(0, 150);
                        return SwingFXUtils.toFXImage(bufferedImage, null);
                    }
                }
            }
        };
    }

    /**
     * Récupère le flux du fichier depuis MinIO
     */
    public InputStream getFileStream(String objectName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }

    /**
     * Obtient l'URL de streaming pour les médias audio/vidéo
     */
    public String getStreamingUrl(String objectName) throws Exception {
        return getPresignedUrl(objectName, 2);
    }

    public String getBucketName() {
        return bucketName;
    }

    public MinioClient getMinioClient() {
        return minioClient;
    }
}
