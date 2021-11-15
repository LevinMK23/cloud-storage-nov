package com.geekbrains.lesson4;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
public class User {
    private int id;
    private String name;
}
