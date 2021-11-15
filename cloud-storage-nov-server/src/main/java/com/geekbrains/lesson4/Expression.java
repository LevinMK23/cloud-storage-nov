package com.geekbrains.lesson4;

@FunctionalInterface
public interface Expression {

    Expression calculate(Expression left, Expression right);

}
