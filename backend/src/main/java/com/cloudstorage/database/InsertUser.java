package com.cloudstorage.database;

public class InsertUser {

    public static void main(String[] args) {

        UserDAO dao = new UserDAO();

        String username = "test";
        String email = "test@mail.com";
        String password = "1234567";
        String first = "test";
        String last = "test";

        // hash password
        String hashedPassword = PasswordUtil.hash(password);

        boolean success = dao.registerUser(username, email, hashedPassword, first, last);

        if (success) {
            System.out.println("User inserted successfully!");
        } else {
            System.out.println("Failed to insert user.");
        }
    }
}
