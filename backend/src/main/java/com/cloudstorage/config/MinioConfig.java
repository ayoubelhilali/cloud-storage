package com.cloudstorage.config;

import io.minio.MinioClient;

public class MinioConfig {
    // UPDATE THESE WITH YOUR ACTUAL CONFIGURATION
    private static final String URL = "http://localhost:9000";
    private static final String ACCESS_KEY = "minioadmin";
    private static final String SECRET_KEY = "minioadmin";

    private static MinioClient instance;

    public static synchronized MinioClient getClient() {
        if (instance == null) {
            instance = MinioClient.builder()
                    .endpoint(URL)
                    .credentials(ACCESS_KEY, SECRET_KEY)
                    .build();
        }
        return instance;
    }
}