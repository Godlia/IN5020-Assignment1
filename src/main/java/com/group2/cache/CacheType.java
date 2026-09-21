package com.group2.cache;


/**
 * Where in the system a cache is used, together with the capacity the
 * assignment requires for that cache.
 *
 * <p>Keeping the capacities here means client and server code never needs to
 * know them: they create caches with
 * {@link Cache#create(CacheType, EvictionPolicy)}.
 */

public enum CacheType {
    /** The client-side cache, which holds up to 45 results. */
    CLIENT(45), 
    /** A server-side cache, one per zone server, which holds up to 150 results. */
    SERVER(150);

    private final int capacity;

    CacheType(int capacity) {
        this.capacity = capacity;
    }

    /**
     * Returns the maximum number of entries for this type of cache.
     *
     * @return the capacity
     */
    public int capacity() {
        return capacity;
    }
}
