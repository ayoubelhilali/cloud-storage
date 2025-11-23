package com.cloudstorage.database;

public class InsertUser {

    private final UserDAO dao = new UserDAO();

    // ✅ Validation
    public String validateInputs(String email, String password) {

        if (email == null || email.trim().isEmpty()) {
            return "Email is required.";
        }

        if (!email.contains("@") || !email.contains(".")) {
            return "Invalid email format.";
        }

        if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return "Invalid email format.";
        }

        if (password == null || password.isEmpty()) {
            return "Password is required.";
        }

        if (!password.matches("^(?=.*[!@#$%^&*()_+=\\-{}\\[\\]:;\"'<>,.?/]).{8,}$")) {
            return "Password must contain at least 8 characters and one special symbol.";
        }

        return null;
    }

    // ✅ Retourne un message d’erreur OU null si succès
    public String registerNewUser(String email, String password, String first, String last) {

        String validationError = validateInputs(email, password);

        if (validationError != null) {
            return validationError;
        }

        String hashedPassword = PasswordUtil.hash(password);

        String username = email.substring(0, email.indexOf("@"));

        boolean success = dao.registerUser(username, email, hashedPassword, first, last);

        if (!success) {
            return "Database error: failed to insert user.";
        }

        return null; // ✅ succès
    }
}
