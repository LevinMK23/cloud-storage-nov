package com.geekbrains.an.model;

import lombok.Data;

@Data
public class ServerHome implements CloudMessage{


    @Override
    public CommandType getType() {
        return CommandType.SERVER_HOME_CLIK;
    }
}
