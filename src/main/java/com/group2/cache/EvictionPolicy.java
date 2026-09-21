package com.group2.cache;

/**
 * Decides which entry a full {@link Cache} removes to make room for a new one.
 */

public enum EvictionPolicy {
    /**
     * First in, first out: evicts the entry that was inserted first.
     * Reading or updating an entry does not change its position.
     * The assignment's Method 1.
     */

    FIFO, 
    /**
     * Least recently used: evicts the entry whose last use is oldest.
     * Both a read and an update count as a use.
     * The assignment's Method 2, which it calls OLDEST.
     */

    LRU
}
