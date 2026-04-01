package org.manu.learning.lambda;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class B_Comparator_N {
    private static final Logger log = LoggerFactory.getLogger(B_Comparator_N.class);

    public static void main(String[] args) {

        User manu = User.builder().name("manu").age(49).build();
        User rim = User.builder().name("rim").age(40).build();
        User robert = User.builder().name("robert").age(42).build();
        User robert2 = User.builder().name("robert").age(41).build();
        User bob = User.builder().name("robert").age(41).build();
        User roger = User.builder().name("robert").age(null).build();
        User adrien = User.builder().name("robert").age(null).build();
        List<User> userList = List.of(bob, roger, manu, adrien, rim);

        log.info(String.valueOf(userList));
        MyComparator<? super User> userComparator = (t1, t2) -> {
            return t1.getAge().compareTo(t2.getAge());
        };

        // log.info(userList.stream().map());
    }
}