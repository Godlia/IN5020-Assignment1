package com.group2.cache;

import java.util.LinkedHashMap;
import java.util.Objects;

public class Cache<K, V> {

    // To implement: capacity
    // To implement: FIFO/LRU
    // To implement: hit/miss
    // To implement: timestamp
    // To implement: Javadoc


    private final LinkedHashMap<K, V> internalMap;
    private final int capacity;
    private final EvictionPolicy policy;

    public Cache(int capacity, EvictionPolicy policy){
        if (capacity <= 0) {
            throw new IllegalArgumentException("Maximum number of entries must be greater than zero.");
        }

        this.capacity = capacity;
        this.policy = Objects.requireNonNull(policy, "Eviction policy must be non-null");
        this.internalMap = new LinkedHashMap<>();
    }

    public void put(K key, V value){
        Objects.requireNonNull(key, "Cache key must be non-null.");
        Objects.requireNonNull(value, "Cache value must be non-null.");

        internalMap.put(key, value);
    }

    public V get(K key){
        Objects.requireNonNull(key, "Cache key must be non-null.");

        return internalMap.get(key);
    }

    public int size(){
        return internalMap.size();
    }

    // **** getters ****



    // **** setters ****










    /* private LinkedHashMap<String, Integer> cache = new LinkedHashMap<>(){
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
            return size() > 150;
        }
    }; */
}
