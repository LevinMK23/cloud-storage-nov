package com.geekbrains.lesson4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamTest {
    public static void main(String[] args) {

        // forEach
        Stream.of(1, 2, 3, 4, 5)
                .forEach(x -> System.out.print(x + " "));
        System.out.println();

        //filter
        Stream.of(1, 2, 3, 4, 5)
                .filter(x -> x % 2 != 0)
                .forEach(x -> System.out.print(x + " "));
        System.out.println();

        // map
        Stream.of(1, 2, 3, 4, 5)
                .filter(x -> x % 2 != 0)
                .map(x -> x + 1)
                .forEach(x -> System.out.print(x + " "));
        System.out.println();

        // map v1
        Stream.of(1, 2, 3, 4, 5)
                .filter(x -> x % 2 != 0)
                .map(StreamTest::repeat)
                .forEach(x -> System.out.print(x + " "));
        System.out.println();

        // collect
        List<Integer> list = Stream.of(1, 2, 3, 4, 5)
                .collect(Collectors.toList());
        System.out.println(list);
        // toList, toSet, toMap

        List<User> users = Arrays.asList(
                new User(1, "Ivan"), new User(2, "Petr"),
                new User(1, "Ivan"), new User(3, "Oleg")
        );

        Set<Integer> ids = users.stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        System.out.println(ids);

        Map<Integer, User> userMap = users.stream()
                .collect(Collectors.toMap(
                        User::getId,
                        Function.identity(),
                        (l, r) -> l
                ));
        System.out.println(userMap);

        Map<Integer, Integer> digits = Stream.of(1, 1, 1, 2, 2, 3, 4, 5, 6, 7, 7, 7, 7, 7, 7)
                .collect(Collectors.toMap(
                        Function.identity(),
                        value -> 1,
                        Integer::sum
                ));
        System.out.println(digits);

        // Stream.of(22).findAny().ifPresent(System.out::println);
        // terminal, continue

        Integer sum = Stream.of(1, 2, 3, 4, 5)
                .reduce(0, Integer::sum);
        System.out.println(sum);

        // toList
        List<Integer> l = Stream.of(1, 2, 3, 4, 5)
                .reduce(
                        new ArrayList<>(),
                        (ar, x) -> {
                            ar.add(x);
                            return ar;
                        },
                        (left, right) -> {
                            left.addAll(right);
                            return left;
                        }
                );
        System.out.println(l);

    }

    static String repeat(int arg) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < arg; i++) {
            s.append('a');
        }
        return s.toString();
    }
}
