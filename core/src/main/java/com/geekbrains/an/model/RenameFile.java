package com.geekbrains.an.model;


import lombok.Data;

@Data
public class RenameFile implements CloudMessage{

    private final String oldName;
    private final String newName;

    public RenameFile(String oldName, String newName) {
        this.oldName = oldName;
        this.newName = newName;
    }

    @Override
    public CommandType getType() {
        return CommandType.RENAME;
    }
}
