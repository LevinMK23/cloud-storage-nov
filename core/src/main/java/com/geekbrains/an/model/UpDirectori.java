package com.geekbrains.an.model;

import lombok.Data;

@Data
public class UpDirectori implements CloudMessage{


    @Override
    public CommandType getType() {
        return CommandType.UP_DIRECTORY;
    }
}
