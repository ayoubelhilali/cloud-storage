package com.cloudstorage.server;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.cloudstorage.service.MinioService;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class SimpleBackend {

    public static void main(String[] args) throws IOException {
        // Crée un serveur sur le port 8080 et bind explicitement sur toutes les interfaces (IPv4/IPv6)
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8080), 0);
        System.out.println("Backend démarré sur http://0.0.0.0:8080 (accessible via localhost)");

        // Route /login
        server.createContext("/login", new LoginHandler());

        // Route /api/minioStatus - returns JSON {"minioConnected": true|false}
        server.createContext("/api/minioStatus", exchange -> {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                // Lightweight TCP check to MinIO (127.0.0.1:9000) instead of using Minio client
                boolean isConnected = false;
                try (java.net.Socket s = new java.net.Socket()) {
                    s.connect(new java.net.InetSocketAddress("127.0.0.1", 9000), 1000);
                    isConnected = true;
                } catch (Exception e) {
                    isConnected = false;
                }

                String resp = "{\"minioConnected\": " + isConnected + "}";
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, resp.getBytes().length);
                try (java.io.OutputStream os = exchange.getResponseBody()) {
                    os.write(resp.getBytes());
                }
            } else {
                String resp = "Method Not Allowed";
                exchange.sendResponseHeaders(405, resp.getBytes().length);
                try (java.io.OutputStream os = exchange.getResponseBody()) {
                    os.write(resp.getBytes());
                }
            }
        });

        // Démarre le serveur
        server.setExecutor(null); // crée un executor par défaut
        server.start();
    }

    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                // Ici tu peux lire le body JSON avec login/password si tu veux
                String response = "{\"status\":\"success\",\"message\":\"Connexion OK\"}";
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                String response = "Use POST method";
                exchange.sendResponseHeaders(405, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }
}
