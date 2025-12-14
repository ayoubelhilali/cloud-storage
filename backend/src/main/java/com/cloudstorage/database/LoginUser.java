package com.cloudstorage.database;

import com.cloudstorage.model.User;
import com.cloudstorage.util.PasswordUtil; // Ensure this import exists
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
        // 1. Hash the password to match the DB format
        // Ensure PasswordUtil.hash() matches how you originally saved the password
        String hashedPassword = PasswordUtil.hash(password);

        // 2. Update SQL to use 'password_hash' column
        String query = "SELECT * FROM users WHERE (email = ? OR username = ?) AND password_hash = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, usernameOrEmail);
            stmt.setString(2, usernameOrEmail);
            stmt.setString(3, hashedPassword); // Compare hash vs hash

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // --- CRITICAL: Fetch ID and other columns ---
                long id = rs.getLong("id");
                String username = rs.getString("username");
                String email = rs.getString("email");
                String fName = rs.getString("first_name");
                String lName = rs.getString("last_name");
                String storedHash = rs.getString("password_hash");

                // Create the User object
                // Note: We typically store the HASH in the user object, not the plain password
                this.loggedUser = new User(id, username, email, storedHash, fName, lName);

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