package com.cloudstorage.database;

import com.cloudstorage.model.User;

public class LoginUser {

    private final UserDAO dao = new UserDAO();
    private User loggedUser;

    public boolean login(String usernameOrEmail, String password) {

        String hashedPassword = PasswordUtil.hash(password);

        User user = dao.login(usernameOrEmail, hashedPassword);

        if (user != null) {
            this.loggedUser = user;
            return true;
        }

        return false;
    }

    public User getLoggedUser() {
        return loggedUser;
    }
}
