// com.cloudstorage.server.Server.java
package com.cloudstorage.server;

import com.cloudstorage.service.MinioService;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.io.OutputStream;

import com.cloudstorage.controller.UserController;

public class Server {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/addUser", UserController::handleInsertUser);

        // New endpoint for MinIO status
        server.createContext("/api/minioStatus", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                MinioService minioService = new MinioService();
                boolean isConnected = minioService.testConnection();
                String response = "{\"minioConnected\": " + isConnected + "}";
                ResponseBuilder.sendResponse(exchange, 200, response, "application/json");
            } else {
                ResponseBuilder.sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
            }
        });

        server.start();
        System.out.println("🚀 Server running on http://localhost:8080");
        // Keep the main thread alive so the HttpServer threads keep serving requests
        try {
            new java.util.concurrent.CountDownLatch(1).await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
