package com.cloudstorage.util;

import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.minio.errors.MinioException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class FileUploader {

    public static void main(String[] args)
            throws IOException, NoSuchAlgorithmException, InvalidKeyException {

        try {
            // Connect to your local MinIO server
            MinioClient minioClient = MinioClient.builder()
                    .endpoint("http://127.0.0.1:9000")
                    .credentials("yahya", "yahya2003")
                    .build();

            // Upload file from Windows path
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket("cloud-storage")             // Existing bucket
                            .object("monfichier-upload.txt")     // Name inside bucket
                            .filename("C:/data/monfichier.txt")  // Local file path
                            .build()
            );

            System.out.println("✔️ Fichier uploadé avec succès vers MinIO !");

        } catch (MinioException e) {
            System.out.println("❌ Une erreur est survenue : " + e);
            try {
                System.out.println("Trace HTTP : " + e.httpTrace());
            } catch (Exception ex) {
                // ignore
            }
        }
    }
}
