package com.cloudstorage.controller;

import com.cloudstorage.service.FileService;
import com.google.gson.Gson; // Assuming you use Gson from your lib folder
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileController implements HttpHandler {

    private final FileService fileService;
    private final Gson gson = new Gson();

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = parseQueryParams(query);

        // --- 1. HANDLE GET REQUESTS (List or Download) ---
        if ("GET".equals(method)) {
            String bucket = params.get("bucket");
            String key = params.get("key"); // Filename

            if (bucket != null && key != null) {
                // A. DOWNLOAD / VIEW IMAGE
                try {
                    InputStream is = fileService.getFileStream(bucket, key);
                    exchange.sendResponseHeaders(200, 0);
                    OutputStream os = exchange.getResponseBody();
                    is.transferTo(os);
                    os.close();
                    is.close();
                } catch (Exception e) {
                    System.err.println("Error streaming file: " + e.getMessage());
                    exchange.sendResponseHeaders(404, -1);
                }
            } else if (bucket != null) {
                // B. LIST FILES (JSON)
                List<Map<String, String>> files = fileService.getUserFiles(bucket);
                String response = gson.toJson(files);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
        // --- 2. HANDLE DELETE REQUESTS ---
        else if ("DELETE".equals(method)) {
            String bucket = params.get("bucket");
            String key = params.get("key");
            String userIdStr = params.get("userId");

            if (bucket != null && key != null && userIdStr != null) {
                try {
                    long userId = Long.parseLong(userIdStr);

                    // Call Service to delete from MinIO and DB
                    fileService.deleteFile(bucket, key, userId);

                    String response = "{\"status\":\"deleted\"}";
                    exchange.sendResponseHeaders(200, response.length());
                    exchange.getResponseBody().write(response.getBytes());
                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(500, -1);
                }
            } else {
                exchange.sendResponseHeaders(400, -1); // Bad Request
            }
        } else {
            exchange.sendResponseHeaders(405, -1); // Method Not Allowed
        }

        exchange.getResponseBody().close();
    }

    // Helper to parse URL params
    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null) return map;
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                map.put(entry[0], entry[1]);
            }
        }
        return map;
    }
}