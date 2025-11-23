package com.cloudstorage.database;

public class LoginUser {

    private final UserDAO dao = new UserDAO();

    /**
     * Attempt login with username/email and password
     * @param usernameOrEmail email or username
     * @param password plain text password
     * @return true if login successful, false otherwise
     */
    public boolean login(String usernameOrEmail, String password) {
        // Hash password
        String hashedPassword = PasswordUtil.hash(password);

        // Call DAO
        return dao.login(usernameOrEmail, hashedPassword);
    }
}
