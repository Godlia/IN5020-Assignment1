package com.group2.cache;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class Cache<K, V> {

    private static final class CacheEntry<T> {
        private T value;
        private Instant lastUsed;

        CacheEntry(T value) {
            this.value = value;
            this.lastUsed = Instant.now();
        }

        void updateTimestamp() {
            lastUsed = Instant.now();
        }

        void updateValue(T newValue) {
            value = newValue;
            updateTimestamp();
        }
    }

    // To implement: cachetype-policy
    // To implement: synchronization
    // To implement: Javadoc

    private static final float LOAD_FACTOR = 0.75f;

    private final LinkedHashMap<K, CacheEntry<V>> internalMap;
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
            protected boolean removeEldestEntry(Map.Entry<K, CacheEntry<V>> eldest) {
                return size() > Cache.this.capacity;
            }
        };
    }

    public V get(K key){
        validateKey(key);
        CacheEntry<V> entry = internalMap.get(key);

        if (entry == null) {
            return null;
        }
        entry.updateTimestamp();
        return entry.value;
    }

    public void put(K key, V value){
        validateKeyAndValue(key, value);
        CacheEntry<V> existing = internalMap.get(key);

        if (existing != null) {
            existing.updateValue(value);
        }
        else {
            internalMap.put(key, new CacheEntry<V>(value));
        }
    }

    public int size(){
        return internalMap.size();
    }

    private void validateKey(K key) {
        Objects.requireNonNull(key, "Cache key must be non-null.");
    }

    private void validateKeyAndValue(K key, V value) {
        validateKey(key);
        Objects.requireNonNull(value, "Cache value must be non-null.");
    }
}
