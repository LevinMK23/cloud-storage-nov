package com.geekbrains.an.model;

import java.io.Serializable;

public interface CloudMessage extends Serializable {
    CommandType getType();
}
