package com.cloudstorage.fx.utils;

import javafx.scene.image.Image;
import java.util.concurrent.ConcurrentHashMap;

public class AvatarCache {
    private static final ConcurrentHashMap<String, Image> cache = new ConcurrentHashMap<>();

    public static void put(String key, Image image) {
        if (key != null && image != null) cache.put(key, image);
    }

    public static Image get(String key) {
        return cache.get(key);
    }

    public static boolean contains(String key) {
        return key != null && cache.containsKey(key);
    }

    public static void remove(String key) {
        cache.remove(key);
    }

    public static void clear() {
        cache.clear();
    }
}