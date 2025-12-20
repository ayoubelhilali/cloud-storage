package com.cloudstorage.database;

import com.cloudstorage.model.User;
import java.sql.*;

public class UserDAO {

    // 1. REGISTER
    public boolean registerUser(String username, String email, String passwordHash, String firstName, String lastName) {
        String sql = "INSERT INTO users (username, email, password_hash, first_name, last_name) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, passwordHash);
            stmt.setString(4, firstName);
            stmt.setString(5, lastName);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Registration error: " + e.getMessage());
            return false;
        }
    }

    // 2. LOGIN (Now fetches avatar_url too)
    public User login(String login, String passwordHash) {
        String sql = "SELECT * FROM users WHERE (username = ? OR email = ?) AND password_hash = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, login);
            stmt.setString(2, login);
            stmt.setString(3, passwordHash);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                long id = rs.getLong("id");
                String uName = rs.getString("username");
                String email = rs.getString("email");
                String fName = rs.getString("first_name");
                String lName = rs.getString("last_name");
                String avatar = rs.getString("profile_picture_url"); // Must match DB column

                return new User(id, uName, email, passwordHash, fName, lName, avatar);
            }
        } catch (SQLException e) {
            System.err.println("Login error: " + e.getMessage());
        }
        return null;
    }

    // 3. UPDATE PROFILE
    public boolean updateProfile(long userId, String firstName, String lastName, String email) {
        String sql = "UPDATE users SET first_name = ?, last_name = ?, email = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            stmt.setString(3, email);
            stmt.setLong(4, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 4. UPDATE AVATAR
    public boolean updateAvatar(long userId, String avatarKey) {
        String sql = "UPDATE users SET profile_picture_url = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, avatarKey);
            stmt.setLong(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 5. CHANGE PASSWORD
    public boolean updatePassword(long userId, String newPasswordHash) {
        // NOTE: In a real app, hash the password before sending it here!
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newPasswordHash);
            stmt.setLong(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 6. DELETE ACCOUNT
    public boolean deleteUser(long userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}