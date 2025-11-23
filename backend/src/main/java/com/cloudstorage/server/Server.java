// com.cloudstorage.server.Server.java
package com.cloudstorage.server;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;

import com.cloudstorage.controller.UserController;

public class Server {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/addUser", UserController::handleInsertUser);
        server.start();
        System.out.println("🚀 Server running on http://localhost:8080");
    }
}
