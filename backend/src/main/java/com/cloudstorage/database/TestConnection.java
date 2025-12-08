package com.cloudstorage.database;

import com.cloudstorage.model.User;

public class TestConnection {

    public static void main(String[] args) {

        UserDAO dao = new UserDAO();

        String username = "babou";
        String password = "123456";

        String hashed = PasswordUtil.hash(password);

        User loggedIn = dao.login(username, hashed);

        if (loggedIn != null) {
            System.out.println("Login success!");
            System.out.println("Welcome " + loggedIn.getLastName());
        } else {
            System.out.println("Invalid username or password.");
        }
    }
}