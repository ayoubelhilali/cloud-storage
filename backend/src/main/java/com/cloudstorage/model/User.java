package com.cloudstorage.model;

public class User {
    private long id;
    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;

    public User() {}

    public User(long id, String username, String email, String password, String firstName, String lastName) {
        this.id = id; // <--- THIS LINE IS CRITICAL
        this.username = username;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() { return username; }

    public String getBucketName() {
        // This ensures the logic is IDENTICAL everywhere in your app
        String fName = (this.firstName == null) ? "" : this.firstName;
        String lName = (this.lastName == null) ? "" : this.lastName;

        // Strategy: "firstname-lastname"
        String bucket = (fName + "-" + lName).toLowerCase().replaceAll("[^a-z0-9-]", "");

        // Fallback: if name is empty, use username
        if (bucket.isEmpty() || bucket.equals("-")) {
            return this.username.toLowerCase().replaceAll("[^a-z0-9-]", "");
        }
        return bucket;
    }
}