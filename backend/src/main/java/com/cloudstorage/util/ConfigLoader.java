package com.cloudstorage.util;

import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    private static Properties properties = new Properties();

    static {
        try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream("config.properties")) {
            properties.load(is);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // Get MinIO configurations:
    public static String getMinioEndpoint() { return properties.getProperty("minio.endpoint"); }
    public static String getMinioAccessKey() { return properties.getProperty("minio.accessKey"); }
    public static String getMinioSecretKey() { return properties.getProperty("minio.secretKey"); }
    public static String getMinioBucketName() { return properties.getProperty("minio.bucketName"); }
    // Get database configurations:
    public static String getDbUrl() {return properties.getProperty("db.url");}
    public static String getDbUser() {return properties.getProperty("db.user");}
    public static String getDbPassword() {return properties.getProperty("db.password");}

}