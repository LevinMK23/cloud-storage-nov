package com.geekbrains.lesson4;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

public class FlatMapTest {

    public static void main(String[] args) throws IOException {
        String words = Files.lines(Paths.get("cloud-storage-nov-server", "server", "text.txt"))
                .flatMap(str -> Arrays.stream(str.split(" +")))
                .filter(str -> !str.isEmpty())
                .collect(Collectors.joining(", "));

        System.out.println(words);
    }
}
