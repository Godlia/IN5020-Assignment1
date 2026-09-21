package com.group2.cache;

public enum CacheType {
    CLIENT(45), 
    SERVER(150);

    private final int capacity;

    CacheType(int capacity) {
        this.capacity = capacity;
    }

    public int capacity() {
        return capacity;
    }
}
