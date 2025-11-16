package com.cloudstorage;

import com.cloudstorage.database.DatabaseConnection;
import com.cloudstorage.service.MinioService;

public class Main {
    public static void main(String[] args) {
        DatabaseConnection.getConnection();
        System.out.println("Connected to database");
        
    }
}
