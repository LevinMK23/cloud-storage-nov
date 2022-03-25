package com.geekbrains.an.model;

import java.io.Serializable;
import java.util.List;

public interface AuthService extends Serializable {
    User findByLoginAndPassword(String login, String password);
    User regNewLoginAndPassword(String login, String password);
    User save(User user);
    User remove(User user);

    List<User> findAll();

}
