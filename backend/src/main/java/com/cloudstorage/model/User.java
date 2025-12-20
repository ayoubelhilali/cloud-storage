package com.cloudstorage.model;

public class User {
    private long id;
    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String avatarUrl; // <--- NEW FIELD

    public User() {}

    public User(long id, String username, String email, String password, String firstName, String lastName, String avatarUrl) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.avatarUrl = avatarUrl;
    }


    // --- Getters & Setters ---

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getBucketName() {
        String fName = (this.firstName == null) ? "" : this.firstName;
        String lName = (this.lastName == null) ? "" : this.lastName;
        String bucket = (fName + "-" + lName).toLowerCase().replaceAll("[^a-z0-9-]", "");
        if (bucket.isEmpty() || bucket.equals("-")) {
            return this.username.toLowerCase().replaceAll("[^a-z0-9-]", "");
        }
        return bucket;
    }
}