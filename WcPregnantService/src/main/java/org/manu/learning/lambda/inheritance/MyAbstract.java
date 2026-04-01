package org.manu.learning.lambda.inheritance;

class MyAbstract {
    private Integer member1;
    private String member2;

    private MyAbstract() {
        member1 = 0;
        member2 = "";
    }

    public class NestedClass extends MyAbstract {

        public NestedClass() {
            super();
        }
    }
}