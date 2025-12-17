package com.cloudstorage.database;

import com.cloudstorage.util.ConfigLoader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    // Get a new database connection
    public static Connection getConnection() {
        String url = ConfigLoader.getDbUrl();
        String user = ConfigLoader.getDbUser();
        String password = ConfigLoader.getDbPassword();

        if (url == null || user == null || password == null) {
            System.err.println("Database configuration missing in config.properties!");
            return null;
        }

        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            System.err.println("Failed to connect to database: " + e.getMessage());
            return null;
        }
    }
}
