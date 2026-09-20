package com.group2.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class Cache<K, V> {

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

    public V get(K key){
        Objects.requireNonNull(key, "Cache key must be non-null.");

        return internalMap.get(key);
    }

    public void put(K key, V value){
        Objects.requireNonNull(key, "Cache key must be non-null.");
        Objects.requireNonNull(value, "Cache value must be non-null.");

        internalMap.put(key, value);
    }

    public int size(){
        return internalMap.size();
    }

    @Override 
    public String toString() {
        return internalMap.toString();
    }

    // **** getters ****



    // **** setters ****

    public static void main(String[] args){
        Cache<String, Integer> cache = new Cache<>(10, EvictionPolicy.FIFO);

        System.out.println(cache);
        System.out.println(cache.get("A"));
        cache.put("A", 1);
        System.out.println(cache);
        System.out.println(cache.get("A"));
        cache.put("B", 2);
        System.out.println(cache);
        cache.put("C", 1);
        System.out.println(cache);
        cache.put("D", 122);
        System.out.println(cache);
        cache.put("E", 1333);
        System.out.println(cache);
        cache.put("F", 1222);
        System.out.println(cache);
        cache.put("G", 1212);
        System.out.println(cache);
        cache.put("H", 12);
        System.out.println(cache);
        cache.put("I", 1225);
        System.out.println(cache);
        cache.put("J", 1622);
        System.out.println(cache);
        cache.put("K", 1262);
        System.out.println(cache);
        System.out.println(cache.size());
        System.out.println(cache.get("P"));


    }









    /* private LinkedHashMap<String, Integer> cache = new LinkedHashMap<>(){
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
            return size() > 150;
        }
    }; */
}
