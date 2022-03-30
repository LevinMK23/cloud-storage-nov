package com.geekbrains.an.model;

import java.util.Objects;

public class User_reg implements CloudMessage {
    private final String login;
    private final String password;

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }




    public User_reg(String login, String password) {
        this.login = login;
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User_reg user = (User_reg) o;

        if (!Objects.equals(login, user.login)) return false;
        if (!Objects.equals(password, user.password)) return false;
        return Objects.equals(login, user.login);
    }

    @Override
    public int hashCode() {
        int result = login != null ? login.hashCode() : 0;
        result = 31 * result + (password != null ? password.hashCode() : 0);
        return result;
    }
    @Override
    public CommandType getType() {
        return CommandType.REGISTER;
    }

}
