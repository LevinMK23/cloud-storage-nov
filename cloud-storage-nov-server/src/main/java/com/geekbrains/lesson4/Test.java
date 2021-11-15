package com.geekbrains.lesson4;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Test {

    public static void main(String[] args) {

        Foo foo = Integer::sum; // method reference

        System.out.println(foo.foo(1, 2));
        System.out.println(foo.getClass());

        // void accept(arg)
        Consumer<String> printer = System.out::println; // peek, forEach
        printer.accept("Hello");

        // boolean test(arg)
        Predicate<Integer> isOdd = arg -> arg % 2 != 0; // filter, anyMatch, noneMath, allMatch
        printer.accept(String.valueOf(isOdd.test(14)));

        // a apply(b) b -> a
        Function<String, Integer> length = String::length; // map, flatMap
        System.out.println(length.apply("OK"));

        // getter T get()
        Supplier<String> getWord = () -> "Hello"; // Collectors

        // Java8 -> Stream, Optional

        // Stream - конвейер данных с доступом в моменте только к одному текущему элементу
        // Optional (value or null)

        Optional<String> user = Optional.of("Oleg");
        user.ifPresent(Test::doAny);

    }

    private static void doAny(String arg) {
        // any
    }
}
