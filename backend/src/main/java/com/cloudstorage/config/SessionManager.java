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
    private static final String KEY_FNAME = "session_fname";
    private static final String KEY_LNAME = "session_lname";

    private SessionManager() {}

    public static void login(User user) {
        if (user == null) return;

        // Safety Check: Refuse to save broken users
        if (user.getId() <= 0) {
            System.err.println("🛑 SessionManager: Attempted to login with invalid User ID: " + user.getId());
            return;
        }

        currentUser = user;
        System.out.println("✅ SessionManager: In-Memory Login for [" + user.getUsername() + "] ID: " + user.getId());

        // Save to Disk
        prefs.putLong(KEY_ID, user.getId());
        prefs.put(KEY_USERNAME, safeString(user.getUsername()));
        prefs.put(KEY_FNAME, safeString(user.getFirstName()));
        prefs.put(KEY_LNAME, safeString(user.getLastName()));

        try {
            prefs.flush(); // Force write to disk immediately
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    public static void logout() {
        currentUser = null;
        try {
            prefs.clear(); // Wipe everything in this node
            prefs.flush();
            System.out.println("⚠️ SessionManager: Logged out and disk cleared.");
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

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
            String fName = prefs.get(KEY_FNAME, "User");
            String lName = prefs.get(KEY_LNAME, "");

            // Rebuild User
            currentUser = new User(savedId, uName, "", "", fName, lName);
            return currentUser;
        }

        // 3. No Session Available
        System.err.println("🛑 CRITICAL: Session is NULL. Returning ID -1.");
        return new User(-1, "Guest", "", "", "Guest", "");
    }

    private static String safeString(String s) { return s == null ? "" : s; }
}