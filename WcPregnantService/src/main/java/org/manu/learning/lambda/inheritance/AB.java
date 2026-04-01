package org.manu.learning.lambda.inheritance;

public class AB implements A, B {
    @Override
    public int size() {
        return A.super.size();
    }
}
