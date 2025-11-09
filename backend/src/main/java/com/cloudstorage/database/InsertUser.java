package com.cloudstorage.database;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class InsertUser {
    public static void main(String[] args) {
        String insertSQL = "INSERT INTO users (name, email, age) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            // Example data
            pstmt.setString(1, "Ayoub Elhilali");
            pstmt.setString(2, "ayoub@example.com");
            pstmt.setInt(3, 20);

            int rows = pstmt.executeUpdate();
            System.out.println("✅ Inserted " + rows + " row(s).");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DatabaseConnection.closeConnection();
        }
    }
}
