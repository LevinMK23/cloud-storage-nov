package com.geekbrains.chat;

import java.util.HashMap;
import java.util.List;

public class UserCache {

    private static UserCache INSTANCE;

    private final HashMap<String, List<String>> cache;
    private String login;

    private UserCache() {
        cache = new HashMap<>();
    }

    public static UserCache getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UserCache();
        }
        return INSTANCE;
    }

    public void put(String userName, List<String> files) {
        cache.put(userName, files);
        login = userName;
    }

    public String getCurrentUser() {
        return login;
    }

    public List<String> getFiles(String userName) {
        return cache.get(userName);
    }

    public void clear() {
        login = null;
        cache.clear();
    }
}
