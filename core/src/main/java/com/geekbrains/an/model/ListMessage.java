package com.geekbrains.an.model;

import lombok.Data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class ListMessage implements CloudMessage{

    private final List<String> files;
    private final String pathField;

    public ListMessage(Path path) throws IOException {
        files = Files.list(path).map(p -> p.getFileName().toString()).collect(Collectors.toList());
        pathField = path.normalize().toAbsolutePath().toString();
    }


    @Override
    public CommandType getType() {
        return CommandType.LIST;
    }
}
