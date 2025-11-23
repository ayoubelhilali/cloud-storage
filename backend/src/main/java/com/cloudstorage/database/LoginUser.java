package com.cloudstorage.database;

public class LoginUser {

    public static void main(String[] args) {

        // Create DAO object
        UserDAO dao = new UserDAO();

        // User credentials to test
        String username = "babou";       // The username you registered
        String password = "123456";      // The plain text password

        // Hash the password the same way you did during registration
        String hashedPassword = PasswordUtil.hash(password);

        // Attempt login
        boolean loggedIn = dao.login(username, hashedPassword);

        // Print result
        if (loggedIn) {
            System.out.println("Login successful!");
        } else {
            System.out.println("Login failed!");
        }
    }
}
