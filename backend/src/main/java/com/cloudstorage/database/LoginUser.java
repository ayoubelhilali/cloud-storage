package com.cloudstorage.database;

import com.cloudstorage.model.User;
import com.cloudstorage.util.PasswordUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginUser {

    private User loggedUser;

    /**
     * Attempts to log in with either Username OR Email.
     */
    public boolean login(String usernameOrEmail, String password) {
        String hashedPassword = PasswordUtil.hash(password);

        String query = "SELECT * FROM users WHERE (email = ? OR username = ?) AND password_hash = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, usernameOrEmail);
            stmt.setString(2, usernameOrEmail);
            stmt.setString(3, hashedPassword);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // Fetch columns
                long id = rs.getLong("id");
                String username = rs.getString("username");
                String email = rs.getString("email");
                String fName = rs.getString("first_name");
                String lName = rs.getString("last_name");
                String storedHash = rs.getString("password_hash");

                // --- NEW: Fetch Avatar ---
                String avatarUrl = rs.getString("profile_picture_url");

                // --- UPDATED: Use the 7-argument constructor ---
                this.loggedUser = new User(id, username, email, storedHash, fName, lName, avatarUrl);

                System.out.println("Login Successful. User ID: " + id);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Login Database Error:");
            e.printStackTrace();
        }
        return false;
    }

    public User getLoggedUser() {
        return loggedUser;
    }
}