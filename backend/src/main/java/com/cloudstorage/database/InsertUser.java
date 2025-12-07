package com.cloudstorage.database;

public class InsertUser {

    public static void main(String[] args) {

        UserDAO dao = new UserDAO();

        String username = "yahya";
        String email = "yahya@mail.com";
        String password = "12345678";
        String first = "Yahya";
        String last = "Azlmat";

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
