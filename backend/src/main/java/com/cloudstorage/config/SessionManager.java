package com.cloudstorage.config;

import com.cloudstorage.model.User;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class SessionManager {
    private static User currentUser;

    // FIX: Use a specific, hardcoded path so all classes share the same storage
    private static final Preferences prefs = Preferences.userRoot().node("cloud_storage_app_session");

    private static final String KEY_ID = "session_id";
    private static final String KEY_USERNAME = "session_username";
    private static final String KEY_EMAIL = "session_email"; // Added Email
    private static final String KEY_FNAME = "session_fname";
    private static final String KEY_LNAME = "session_lname";
    private static final String KEY_AVATAR = "session_avatar"; // NEW: To persist avatar

    private static boolean favoritesChanged = true;

    private SessionManager() {}

    // --- FAVORITES SYNC LOGIC ---
    public static boolean isFavoritesChanged() { return favoritesChanged; }
    public static void setFavoritesChanged(boolean changed) { favoritesChanged = changed; }

    // --- LOGIN LOGIC ---
    public static void login(User user) {
        if (user == null) return;

        if (user.getId() <= 0) {
            System.err.println("🛑 SessionManager: Attempted to login with invalid User ID: " + user.getId());
            return;
        }

        currentUser = user;
        System.out.println("✅ SessionManager: In-Memory Login for [" + user.getUsername() + "] ID: " + user.getId());

        // Save to Disk
        prefs.putLong(KEY_ID, user.getId());
        prefs.put(KEY_USERNAME, safeString(user.getUsername()));
        prefs.put(KEY_EMAIL, safeString(user.getEmail()));
        prefs.put(KEY_FNAME, safeString(user.getFirstName()));
        prefs.put(KEY_LNAME, safeString(user.getLastName()));
        prefs.put(KEY_AVATAR, safeString(user.getAvatarUrl())); // SAVE AVATAR

        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    // --- LOGOUT LOGIC ---
    public static void logout() {
        currentUser = null;
        try {
            prefs.clear();
            prefs.flush();
            System.out.println("⚠️ SessionManager: Logged out and disk cleared.");
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    // --- SESSION RECOVERY ---
    public static User getCurrentUser() {
        // 1. Check Memory
        if (currentUser != null) {
            return currentUser;
        }

        // 2. Check Disk (Restart Recovery)
        long savedId = prefs.getLong(KEY_ID, -1);

        if (savedId > 0) {
            System.out.println("🔄 SessionManager: Recovering session from Disk (ID: " + savedId + ")");
            String uName = prefs.get(KEY_USERNAME, "user");
            String email = prefs.get(KEY_EMAIL, "");
            String fName = prefs.get(KEY_FNAME, "User");
            String lName = prefs.get(KEY_LNAME, "");
            String avatar = prefs.get(KEY_AVATAR, ""); // RECOVER AVATAR

            // Rebuild User using the 7-argument constructor
            // (ID, Username, Email, Password(empty), FirstName, LastName, Avatar)
            currentUser = new User(savedId, uName, email, "", fName, lName, avatar);
            return currentUser;
        }

        return null;
    }

    // --- DYNAMIC UPDATES ---
    public static void updateCurrentAvatar(String newAvatarUrl) {
        if (currentUser != null) {
            currentUser.setAvatarUrl(newAvatarUrl);
            prefs.put(KEY_AVATAR, safeString(newAvatarUrl)); // Update disk immediately
            try { prefs.flush(); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private static String safeString(String s) { return s == null ? "" : s; }
}