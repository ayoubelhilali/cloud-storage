package com.cloudstorage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Main {
    public static void main(String[] args) {
        // Replace with your Neon connection details
        String url = "jdbc:postgresql://ep-ancient-bonus-ah5am2mw-pooler.c-3.us-east-1.aws.neon.tech:5432/neondb";
        String user = "neondb_owner";
        String password = "npg_gNFXh36eDVMO";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            System.out.println("✅ Connected to Neon PostgreSQL successfully!");

            // 1. Create table
            String createTableSQL = """
                    CREATE TABLE IF NOT EXISTS test_users (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(50),
                        email VARCHAR(50)
                    );
                    """;
            stmt.execute(createTableSQL);
            System.out.println("✅ Table 'test_users' created (if it didn't exist).");

            // 2. Insert a row
            String insertSQL = """
                    INSERT INTO test_users (name, email) 
                    VALUES ('Alice', 'alice@example.com');
                    """;
            stmt.executeUpdate(insertSQL);
            System.out.println("✅ Inserted a row into 'test_users'.");

            // 3. Read the table
            String selectSQL = "SELECT * FROM test_users;";
            ResultSet rs = stmt.executeQuery(selectSQL);

            System.out.println("📋 Current data in 'test_users':");
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String email = rs.getString("email");
                System.out.printf("ID: %d, Name: %s, Email: %s%n", id, name, email);
            }

            /*// 4. (Optional) Update a row
            String updateSQL = "UPDATE test_users SET name='Alice Updated' WHERE id=1;";
            stmt.executeUpdate(updateSQL);
            System.out.println("✅ Updated a row.");

           */ // 5. (Optional) Delete a row
            String deleteSQL = "DELETE FROM test_users WHERE id=5;";
            stmt.executeUpdate(deleteSQL);
            System.out.println("✅ Deleted a row.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}