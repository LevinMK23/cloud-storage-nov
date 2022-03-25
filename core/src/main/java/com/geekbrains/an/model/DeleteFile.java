package com.geekbrains.an.model;


import lombok.Data;

@Data
public class DeleteFile implements CloudMessage{

    private final String fileName;


    public DeleteFile(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public CommandType getType() {
        return CommandType.DELETE;
    }
}
