package com.geekbrains.an.model;


import lombok.Data;

@Data
public class PasteFile implements CloudMessage{

    private final String fileName;
    private final String path_copy;

    public PasteFile(String fileName, String path_copy) {
        this.fileName = fileName;
        this.path_copy = path_copy;
    }

    @Override
    public CommandType getType() {
        return CommandType.PASTE_FILE;
    }
}
