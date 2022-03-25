package com.geekbrains.an.model;


import lombok.Data;

@Data
public class CreateFolder implements CloudMessage{

    private final String fileName;

    public CreateFolder(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public CommandType getType() {
        return CommandType.CREATE_FOLDER;
    }
}
