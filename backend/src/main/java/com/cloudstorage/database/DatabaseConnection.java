package com.cloudstorage.database;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private static Connection connection = null;

    public static Connection getConnection() {
        if (connection == null) {
            try {
                Properties props = new Properties();

                // ⚡ Load config.properties from resources folder (Maven-friendly)
                try (InputStream input = DatabaseConnection.class.getResourceAsStream("/config.properties")) {
                    if (input == null) {
                        System.err.println("❌ config.properties not found in resources folder!");
                        return null;
                    }
                    props.load(input);
                }

                String url = props.getProperty("db.url");
                String user = props.getProperty("db.user");
                String password = props.getProperty("db.password");

                // Load PostgreSQL driver and connect
                Class.forName("org.postgresql.Driver");
                connection = DriverManager.getConnection(url, user, password);
                System.out.println("✅ Database connection successful!");
            } catch (ClassNotFoundException | SQLException e) {
                System.err.println("❌ Connection failed: " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                System.err.println("❌ Error loading properties: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return connection;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("🔒 Connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
