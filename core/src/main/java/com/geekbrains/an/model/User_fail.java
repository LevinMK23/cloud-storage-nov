package com.geekbrains.an.model;

import java.util.Objects;

public class User_fail implements CloudMessage {

    @Override
    public CommandType getType() {
        return CommandType.AUTH_FAIL;
    }

}
