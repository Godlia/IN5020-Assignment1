package com.group2.cache;

import java.util.LinkedHashMap;

public class Cache<K, V> {

    // To implement: capacity
    // To implement: FIFO/LRU
    // To implement: hit/miss
    // To implement: timestamp
    // To implement: Javadoc


    final private LinkedHashMap<K, V> map;
    final int capacity;
    final private EvictionPolicy policy;

    public Cache(int capacity, EvictionPolicy policy){
        this.capacity = capacity;
        this.policy = policy;
        this.map = new LinkedHashMap<>();
    }

    public void put(K key, V value){
        map.put(key, value);
    }

    public V get(K key){
        return map.get(key);
    }








    /* private LinkedHashMap<String, Integer> cache = new LinkedHashMap<>(){
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
            return size() > 150;
        }
    }; */
}
