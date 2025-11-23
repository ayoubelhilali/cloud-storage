package com.cloudstorage.database;

public class TestConnection {

    public static void main(String[] args) {

        UserDAO dao = new UserDAO();

        String username = "babou";
        String password = "123456";

        String hashed = PasswordUtil.hash(password);

        boolean loggedIn = dao.login(username, hashed);

        if (loggedIn) {
            System.out.println("Login success!");
        } else {
            System.out.println("Invalid username or password.");
        }
    }
}
