package com.cloudstorage.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL = "jdbc:postgresql://ep-ancient-bonus-ah5am2mw-pooler.c-3.us-east-1.aws.neon.tech:5432/neondb";
    private static final String USER = "neondb_owner";
    private static final String PASSWORD = "npg_gNFXh36eDVMO";

    // Get a new database connection
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            // Log error and return null if connection fails
            System.err.println("Failed to connect to database: " + e.getMessage());
            return null;
        }
    }
}
