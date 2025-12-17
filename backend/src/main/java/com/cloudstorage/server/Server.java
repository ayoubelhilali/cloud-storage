package com.cloudstorage.server;

import com.cloudstorage.controller.FileController; // Make sure you have this class
import com.cloudstorage.controller.UserController;
import com.cloudstorage.service.FileService;       // Make sure you have this class
import com.cloudstorage.service.MinioService;
import com.sun.net.httpserver.HttpServer;
import io.minio.MinioClient;

import java.net.InetSocketAddress;

public class Server {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // --- 1. SETUP MINIO CLIENT (Backend) ---
        // We need a specific client instance to pass to our FileService
        MinioClient minioClient = MinioClient.builder()
                .endpoint("http://localhost:9000")
                .credentials("minioadmin", "minioadmin")
                .build();

        // --- 2. INITIALIZE SERVICES ---
        FileService fileService = new FileService(minioClient);
        FileController fileController = new FileController(fileService);

        // --- 3. REGISTER ENDPOINTS ---
        server.createContext("/api/addUser", UserController::handleInsertUser);

        // Existing status endpoint
        server.createContext("/api/minioStatus", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                MinioService ms = new MinioService();
                boolean isConnected = ms.testConnection();
                String response = "{\"minioConnected\": " + isConnected + "}";
                ResponseBuilder.sendResponse(exchange, 200, response, "application/json");
            } else {
                ResponseBuilder.sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
            }
        });

        // --- 4. NEW: REGISTER THE FILES ENDPOINT ---
        // This connects http://localhost:8080/files -> FileController
        server.createContext("/files", fileController);

        server.start();
        System.out.println("🚀 Server running on http://localhost:8080");
        System.out.println("   - Endpoint: /api/addUser");
        System.out.println("   - Endpoint: /files (Ready for Dashboard)");

        // Keep alive
        try {
            new java.util.concurrent.CountDownLatch(1).await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}