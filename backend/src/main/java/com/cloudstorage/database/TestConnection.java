package com.cloudstorage.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestConnection {
    public static void main(String[] args) {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT version();");
            if (rs.next()) {
                System.out.println("🟢 Connected to PostgreSQL: " + rs.getString(1));
            }

        } catch (Exception e) {
            System.err.println("⚠️ Test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DatabaseConnection.closeConnection();
        }
    }
}
