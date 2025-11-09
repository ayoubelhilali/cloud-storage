package com.cloudstorage.database;

import java.sql.Connection;
import java.sql.Statement;

public class DatabaseSetup {
    public static void main(String[] args) {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1️⃣ Create table
            String createTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(100),
                    email VARCHAR(100),
                    age INT
                );
                """;
            stmt.executeUpdate(createTable);
            System.out.println("✅ Table 'users' created successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DatabaseConnection.closeConnection();
        }
    }
}
