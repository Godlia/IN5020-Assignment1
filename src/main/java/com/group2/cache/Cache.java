package com.group2.cache;

import java.util.LinkedHashMap;
import java.util.Objects;

public class Cache<K, V> {

    // To implement: capacity
    // To implement: FIFO/LRU
    // To implement: hit/miss
    // To implement: timestamp
    // To implement: Javadoc


    final private LinkedHashMap<K, V> internalMap;
    final private int capacity;
    final private EvictionPolicy policy;

    public Cache(int capacity, EvictionPolicy policy){
        if (capacity <= 0) {
            throw new IllegalArgumentException("Maximum number of entries must be greater than zero.");
        }

        this.capacity = capacity;
        this.policy = Objects.requireNonNull(policy);
        this.internalMap = new LinkedHashMap<>();
    }

    public void put(K key, V value){
        internalMap.put(key, value);
    }

    public V get(K key){
        return internalMap.get(key);
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
