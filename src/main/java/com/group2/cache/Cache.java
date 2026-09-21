package com.group2.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class Cache<K, V> {

    // To implement: timestamp
    // To implement: synchronization
    // To implement: Javadoc

    private static final float LOAD_FACTOR = 0.75f;

    private final LinkedHashMap<K, V> internalMap;
    private final int capacity;
    private final EvictionPolicy policy;

    public Cache(int capacity, EvictionPolicy policy){
        if (capacity <= 0) {
            throw new IllegalArgumentException("Maximum number of entries must be greater than zero.");
        }

        this.capacity = capacity;   

        this.policy = Objects.requireNonNull(policy, "Eviction policy must be non-null");
        boolean accessOrder = switch (policy) {
            case FIFO -> false;
            case LRU -> true;
        };

        int initialCapacity = (int) Math.ceil(capacity + 1/LOAD_FACTOR);

        this.internalMap = new LinkedHashMap<>(initialCapacity, LOAD_FACTOR, accessOrder){
            @Override 
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > Cache.this.capacity;
            }
        };
    }

    public V get(K key){
        validateKey(key);
        return internalMap.get(key);
    }

    public void put(K key, V value){
        validateKeyAndValue(key, value);
        internalMap.put(key, value);
    }

    public int size(){
        return internalMap.size();
    }

    @Override 
    public String toString() {
        return internalMap.toString();
    }

    private void validateKey(K key) {
        Objects.requireNonNull(key, "Cache key must be non-null.");
    }

    private void validateKeyAndValue(K key, V value) {
        validateKey(key);
        Objects.requireNonNull(value, "Cache value must be non-null.");
    }
}
