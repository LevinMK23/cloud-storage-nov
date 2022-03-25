package com.geekbrains.an.netty;

import com.geekbrains.an.model.AuthService;
import com.geekbrains.an.model.User;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DBAuthService implements AuthService {

     private static DBAuthService INSTANCE;

    private final CopyOnWriteArrayList<User> users = new CopyOnWriteArrayList<>();

    private DBAuthService(User user) {

            users.add(user);

    }

//    public static DBAuthService getInstance() {
//        if (INSTANCE == null) {
//            synchronized (DBAuthService.class) {
//                if (INSTANCE == null) {
//                    INSTANCE = new DBAuthService();
//                }
//            }
//        }
//        return INSTANCE;
//    }

    @Override
    public User findByLoginAndPassword(String login, String password) {
        for (User u : users) {
            if (u.getLogin().equals(login) && u.getPassword().equals(password)) {
                return u;
            }
        }
        return null;
    }

    @Override
    public User regNewLoginAndPassword(String login, String password) {
        for (User u : users) {
            if (!u.getLogin().equals(login)) {
                users.add(u);
                return u;
            }
        }
        return null;
    }


    @Override
    public User save(User user) {
        users.add(user);
        return user;
    }

    @Override
    public User remove(User user) {
        users.remove(user);
        return null;
    }
    @Override
    public List<User> findAll() {
        return null;
    }

}
