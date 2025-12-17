package com.cloudstorage.controller;
import com.cloudstorage.server.Server;
import com.cloudstorage.database.DatabaseConnection;
import com.cloudstorage.model.User;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.sql.*;
import java.security.MessageDigest;

public class UserController {

    public static void handleInsertUser(HttpExchange exchange) throws IOException {

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            return;
        }

        // Convert JSON → User object
        Gson gson = new Gson();
        User user = gson.fromJson(new InputStreamReader(exchange.getRequestBody()), User.class);

        Connection conn = null;
        try {
            // --- Handle database connection ---
            conn = DatabaseConnection.getConnection();

            // 1) Check if user exists by username or email
            String checkSql = "SELECT COUNT(*) FROM users WHERE username = ? OR email = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, user.getUsername());
                checkStmt.setString(2, user.getEmail());

                try (ResultSet rs = checkStmt.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        sendResponse(exchange, 409, "User already exists");
                        return;
                    }
                }
            }

            // 2) Hash password
            String hashedPassword = hashPassword(user.getPassword());

            // 3) Insert user
            String insertSql = """
                    INSERT INTO users (username, email, password_hash, first_name, last_name)
                    VALUES (?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.setString(1, user.getUsername());
                stmt.setString(2, user.getEmail());
                stmt.setString(3, hashedPassword);
                stmt.setString(4, user.getFirstName());
                stmt.setString(5, user.getLastName());
                stmt.executeUpdate();
            }

            sendResponse(exchange, 200, "User registered successfully");

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        } finally {
            // Close connection safely
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException ignored) {}
        }
    }

    // Utility: send response
    private static void sendResponse(HttpExchange exchange, int status, String message) throws IOException {
        byte[] bytes = message.getBytes();
        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // Utility: SHA-256 password hashing
    private static String hashPassword(String password) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(password.getBytes());
        byte[] bytes = md.digest();
        StringBuilder sb = new StringBuilder();

        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}