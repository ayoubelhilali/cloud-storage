package com.cloudstorage.database;

import java.sql.*;

public class UserDAO {

    // ---------- REGISTER ----------
    public boolean registerUser(String username, String email,
                                String passwordHash,
                                String firstName, String lastName) {

        String sql = "INSERT INTO users (username, email, password_hash, first_name, last_name) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, passwordHash);
            stmt.setString(4, firstName);
            stmt.setString(5, lastName);

            stmt.executeUpdate();
            System.out.println("User registered successfully!");
            return true;

        } catch (SQLException e) {
            System.out.println("Registration error: " + e.getMessage());
            return false;
        }
    }


    // ---------- LOGIN ----------
    public boolean login(String username, String passwordHash) {

        String sql = "SELECT * FROM users WHERE username = ? AND password_hash = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, passwordHash);

            ResultSet rs = stmt.executeQuery();

            return rs.next(); // true if user exists

        } catch (SQLException e) {
            System.out.println("Login error: " + e.getMessage());
            return false;
        }
    }
}
