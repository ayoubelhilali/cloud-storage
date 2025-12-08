package com.cloudstorage.database;

import com.cloudstorage.model.User;

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
    public User login(String login, String passwordHash) {

        String sql = "SELECT * FROM users WHERE (username = ? OR email = ?) AND password_hash = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, login);
            stmt.setString(2, login);
            stmt.setString(3, passwordHash);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setFirstName(rs.getString("first_name"));
                user.setLastName(rs.getString("last_name"));
                return user;
            }

            return null;

        } catch (SQLException e) {
            System.out.println("Login error: " + e.getMessage());
            return null;
        }
    }

}
