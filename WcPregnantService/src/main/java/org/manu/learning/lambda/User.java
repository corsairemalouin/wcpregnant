package org.manu.learning.lambda;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class User {
    String name;
    Integer age;
}
