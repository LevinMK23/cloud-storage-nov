package com.geekbrains.an.model;

import java.util.Objects;

public class User_reg_fail implements CloudMessage {



    @Override
    public CommandType getType() {
        return CommandType.REGISTER_FAIL;
    }

}
