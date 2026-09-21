package com.group2.cache;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;


/**
 * A fixed-capacity, thread-safe cache mapping keys to values, with a
 * configurable eviction policy.
 *
 * <p>When inserting a new key would exceed the capacity, one entry is evicted
 * according to the cache's {@link EvictionPolicy}:
 * <ul>
 *     <li>{@link EvictionPolicy#FIFO FIFO}: the entry that was inserted first is
 *         evicted. Reading or updating an entry does not change its position.</li>
 *     <li>{@link EvictionPolicy#LRU LRU}: the entry that was used least recently
 *         is evicted. Both a {@link #get} hit and a {@link #put} update count as
 *         a use. This is the policy the assignment calls OLDEST.</li>
 * </ul>
 *
 * <p>As required by the assignment, every entry records the time it was last
 * used. The eviction order itself is maintained by a {@link LinkedHashMap}
 * (insertion order for FIFO, access order for LRU), so the entry to evict is
 * always the eldest entry in the map and is found in O(1), without scanning
 * timestamps. Under LRU, the eldest entry is exactly the entry with the oldest
 * last-used timestamp. The linked order is also more precise than the
 * timestamps: it keeps the true order of uses that happen within the same
 * clock tick, and it is unaffected by adjustments to the system clock.
 *
 * <p>Null keys and null values are rejected. As a result, {@link #get}
 * returning {@code null} always means a cache miss, never a stored null.
 *
 * <p>All methods that access the entries are synchronized, so one instance can
 * safely be shared between threads. Under LRU, even {@link #get} modifies the
 * internal order, which is why reads are synchronized as well as writes.
 *
 * <p>Instances are created with {@link #create(CacheType, EvictionPolicy)}.
 *
 * @param <K> the type of keys
 * @param <V> the type of cached values
 */
public final class Cache<K, V> {

    /**
     * A cached value together with the time it was last used.
     * Static because an entry never needs a reference to the cache holding it.
     *
     * @param <T> the type of the cached value
     */
    private static final class CacheEntry<T> {
        private T value;
        private Instant lastUsed;

        /** Creates an entry and records the current time as its last use. */
        CacheEntry(T value) {
            this.value = value;
            this.lastUsed = Instant.now();
        }

        /** Records the current time as the entry's last use. */
        void updateTimestamp() {
            lastUsed = Instant.now();
        }

        /** Replaces the value and records the current time as the entry's last use. */
        void updateValue(T newValue) {
            value = newValue;
            updateTimestamp();
        }
    }

    /** Load factor of the internal map; the standard HashMap default. */
    private static final float LOAD_FACTOR = 0.75f;

    private final LinkedHashMap<K, CacheEntry<V>> internalMap;
    private final int capacity;
    private final EvictionPolicy policy;

    /**
     * Creates an empty cache with the capacity configured for the given type.
     *
     * @param type   where the cache is used (client or server); determines its capacity
     * @param policy the eviction policy to apply when the cache is full
     * @param <K>    the type of keys
     * @param <V>    the type of cached values
     * @return a new, empty cache
     * @throws NullPointerException if {@code type} or {@code policy} is null
     */
    public static <K, V> Cache<K, V> create(CacheType type, EvictionPolicy policy) {
        Objects.requireNonNull(type, "Cache type must be non-null");
        return new Cache<>(type.capacity(), policy);
    }

    /**
     * Creates an empty cache with an explicit capacity.
     *
     * <p>Package-private: code outside this package should use
     * {@link #create(CacheType, EvictionPolicy)}, so that capacities are defined
     * in one place ({@link CacheType}). This constructor exists for tests.
     *
     * @param capacity the maximum number of entries; must be positive
     * @param policy   the eviction policy to apply when the cache is full
     * @throws IllegalArgumentException if {@code capacity} is zero or negative
     * @throws NullPointerException     if {@code policy} is null
     */
    Cache(int capacity, EvictionPolicy policy){
        if (capacity <= 0) {
            throw new IllegalArgumentException("Maximum number of entries must be greater than zero.");
        }
        this.policy = Objects.requireNonNull(policy, "Eviction policy must be non-null");

        // LinkedHashMap keeps its entries in insertion order by default (FIFO),
        // or in access order if requested (LRU). Either way, the eldest entry
        // is the one the policy says to evict.
        boolean accessOrder = switch (policy) {
            case FIFO -> false;
            case LRU -> true;
        };

        this.capacity = capacity;   

        // The map briefly holds capacity + 1 entries before evicting, so it is
        // sized for that many to avoid resizing the moment the cache fills up.
        int initialCapacity = (int) Math.ceil((capacity + 1)/LOAD_FACTOR);

        this.internalMap = new LinkedHashMap<>(initialCapacity, LOAD_FACTOR, accessOrder){
            // Called by LinkedHashMap after every insertion of a new key.
            // Returning true removes the eldest entry, which keeps the cache
            // at or below its capacity.
            @Override 
            protected boolean removeEldestEntry(Map.Entry<K, CacheEntry<V>> eldest) {
                return size() > Cache.this.capacity;
            }
        };
    }

    /**
     * Returns the value cached for {@code key}, or {@code null} on a cache miss.
     *
     * <p>A hit updates the entry's last-used timestamp. Under LRU it also makes
     * the entry the most recently used; under FIFO its eviction position is
     * unchanged. A miss leaves the cache unchanged.
     *
     * @param key the key to look up; must not be null
     * @return the cached value, or {@code null} if the key is not in the cache
     * @throws NullPointerException if {@code key} is null
     */
    public synchronized V get(K key){
        validateKey(key);
        CacheEntry<V> entry = internalMap.get(key);

        if (entry == null) {
            return null;
        }
        entry.updateTimestamp();
        return entry.value;
    }

    /**
     * Stores {@code value} under {@code key}.
     *
     * <p>If the key is already cached, its value is replaced and its last-used
     * timestamp updated. This never changes the number of entries or causes an
     * eviction. Under LRU the entry becomes the most recently used; under FIFO it
     * keeps its original insertion position.
     *
     * <p>If the key is new and the cache is full, the entry chosen by the
     * eviction policy is removed to make room.
     *
     * @param key   the key to store the value under; must not be null
     * @param value the value to cache; must not be null
     * @throws NullPointerException if {@code key} or {@code value} is null
     */
    public synchronized void put(K key, V value){
        validateKeyAndValue(key, value);

        // Under LRU, this lookup also moves an existing entry to the most
        // recently used position, so an update counts as a use.
        CacheEntry<V> existing = internalMap.get(key);

        if (existing != null) {
            existing.updateValue(value);
        }
        else {
            internalMap.put(key, new CacheEntry<>(value));
        }
    }

    /**
     * Returns the number of entries currently in the cache.
     *
     * @return the number of cached entries, never more than the capacity
     */
    public synchronized int size(){
        return internalMap.size();
    }

    /**
     * Returns the eviction policy this cache was created with.
     *
     * @return the eviction policy
     */
    public EvictionPolicy getEvictionPolicy(){
        return this.policy;
    }

    private void validateKey(K key) {
        Objects.requireNonNull(key, "Cache key must be non-null.");
    }

    private void validateKeyAndValue(K key, V value) {
        validateKey(key);
        Objects.requireNonNull(value, "Cache value must be non-null.");
    }


        /**
     * Returns the last-used timestamp of the entry for {@code key}, or
     * {@code null} if the key is not cached.
     *
     * <p>Package-private, for tests. It iterates over the entries instead of
     * calling {@link LinkedHashMap#get}, because under LRU a {@code get} counts
     * as a use and would change the eviction order.
     *
     * @param key the key whose timestamp to return
     * @return the time the entry was last used, or {@code null} if absent
     */
    synchronized Instant lastUsed(K key) {
        for (Map.Entry<K, CacheEntry<V>> e : internalMap.entrySet()) {
            if (e.getKey().equals(key)) {
                return e.getValue().lastUsed;
            }
        }
        return null;
    }

}
