package org.manu.learning.lambda;

@FunctionalInterface
public interface MyComparator<T> {

    int compare(T t1, T t2);

}
