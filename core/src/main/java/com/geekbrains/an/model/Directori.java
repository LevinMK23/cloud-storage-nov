package com.geekbrains.an.model;

import lombok.Data;

import java.nio.file.Path;
import java.nio.file.Paths;

@Data
public class Directori implements CloudMessage{

    private final String fileName;



    public Directori(String fileName) {
        this.fileName = fileName;
    }



    @Override
    public CommandType getType() {
        return CommandType.IN_DIRECTORY;
    }
}
