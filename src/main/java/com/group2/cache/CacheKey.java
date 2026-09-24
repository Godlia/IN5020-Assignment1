package com.group2.cache;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public record CacheKey(String methodName, List<Object> arguments) {

    public CacheKey {
        Objects.requireNonNull(methodName, "methodName cannot be null");
        if (methodName.isBlank()) {
            throw new IllegalArgumentException("methodName cannot be blank");
        }
        arguments = List.copyOf(arguments);
    }

    public static CacheKey of(String methodName, Object... arguments) {
        Objects.requireNonNull(arguments, "arguments");
        return new CacheKey(methodName, Arrays.asList(arguments));
    }
}
