package com.cloudstorage.controller;

import com.cloudstorage.database.DatabaseConnection;
import com.cloudstorage.model.User;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserController {

    public static void handleInsertUser(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            return;
        }

        Gson gson = new Gson();
        User user = gson.fromJson(new InputStreamReader(exchange.getRequestBody()), User.class);

        Connection conn = DatabaseConnection.getConnection(); // ❌ Do NOT use try-with-resources here
        if (conn == null) {
            sendResponse(exchange, 503, "Database connection failed");
            return;
        }

        try {
            // 1️⃣ Check if user already exists (by email)
            String checkSql = "SELECT COUNT(*) FROM users WHERE email = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, user.getEmail());   // ✅ set the parameter first
                try (ResultSet rs = checkStmt.executeQuery()) {   // then execute
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        sendResponse(exchange, 409, "User with this email already exists");
                        return;
                    }
                }
            }
            // 2️⃣ Insert user
            String insertSql = "INSERT INTO users(name, email, age) VALUES (?, ?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, user.getName());
                insertStmt.setString(2, user.getEmail());
                insertStmt.setInt(3, user.getAge());
                insertStmt.executeUpdate();
            }

            // 3️⃣ Send success
            sendResponse(exchange, 200, "User added successfully");

        } catch (SQLException e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "Database error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    private static void sendResponse(HttpExchange exchange, int status, String message) throws IOException {
        byte[] bytes = message.getBytes();
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
