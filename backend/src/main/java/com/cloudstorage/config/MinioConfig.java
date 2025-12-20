package com.cloudstorage.config;

import io.minio.MinioClient;

public class MinioConfig {
    // UPDATE THESE WITH YOUR ACTUAL CONFIGURATION
    private static final String URL = "https://bucket-production-f478.up.railway.app:443";
    private static final String ACCESS_KEY = "cloud_storage";
    private static final String SECRET_KEY = "cloud_storage123@";

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