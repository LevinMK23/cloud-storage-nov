package com.geekbrains.lesson4;

@FunctionalInterface
public interface Foo {

    int foo(int a, int b);

    default void bar() {
        System.out.println("Hello world");
    }

}
