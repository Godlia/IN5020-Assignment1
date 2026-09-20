package com.group2.cache;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

public class CacheTest {

    // ***** Invariant checks *****

    @ParameterizedTest
    @EnumSource(EvictionPolicy.class)
    @Disabled("Cache class not fully set up yet")
    void rejectsZeroCapacity(EvictionPolicy policy) {
    }

    @ParameterizedTest
    @EnumSource(EvictionPolicy.class)
    @Disabled("Cache class not fully set up yet")
    void rejectsNegativeCapacity() {
    }

    @ParameterizedTest
    @EnumSource(EvictionPolicy.class)
    @Disabled("Cache class not fully set up yet")
    void rejectsNullEvictionPolicy(EvictionPolicy policy){
    }

    @ParameterizedTest
    @EnumSource(EvictionPolicy.class)
    @Disabled("Cache class not fully set up yet")
    void rejectsNullKey() {
    }

    @ParameterizedTest
    @EnumSource(EvictionPolicy.class)
    @Disabled("Cache class not fully set up yet")
    void rejectsNullValue() {
    }


    // ***** General behavior *****

    @ParameterizedTest
    @EnumSource(EvictionPolicy.class)
    void storesAndRetrievesValue(EvictionPolicy policy) {

        Cache<String, Integer> cache = new Cache<>(5, policy);

        cache.put("A", 1);
        Integer result = cache.get("A");

        assertEquals(1, result);
    }
    

    // What happens if the same key is inserted twice?
    // Why test this? Otherwise, the second put() could be treated as a brand-new cache entry. One key should represent one mapping.
    // assertEquals(1, cache.size());
    @ParameterizedTest
    @EnumSource(EvictionPolicy.class)
    @Disabled("Cache class not fully set up yet")
    void putUpdatesExistingValue() {
    }


    // Edge case test
    // Checking that the cache is able to handle a cache with capacity of one
    // Boundary values often exposes bugs
    @ParameterizedTest
    @EnumSource(EvictionPolicy.class)
    @Disabled("Cache class not fully set up yet")
    void handlesCapacityOfOne() {
    }


    // Checks that eviction doesnt just work once
    // Testing invariant preservation over time
    @ParameterizedTest
    @EnumSource(EvictionPolicy.class)
    @Disabled("Cache class not fully set up yet")
    void continuesRespectingCapacityAfterMultipleEvictions() {
    }

    @ParameterizedTest
    @EnumSource(EvictionPolicy.class)
    @Disabled("Cache class not fully set up yet")
    void returnsNullForCacheMiss() {
    }

    // ***** FIFO behavior *****

    @Test
    @Disabled("FIFO has not been implemented yet")
    void fifoEvictsFirstInsertedEntry() {
    }

    @Test
    @Disabled("FIFO has not been implemented yet")
    void fifoAccessDoesNotUpdateEvictionOrder() {
    }

    @Test
    @Disabled("FIFO has not been implemented yet")
    void fifoUpdatingEntryDoesNotChangeEvictionOrder() {
    }

    // ***** LRU behavior *****

    @Test
    @Disabled("LRU has not been implemented yet")
    void lruEvictsLeastRecentlyUsedEntry() {
    }

    @Test
    @Disabled("LRU has not been implemented yet")
    void lruAccessUpdatesEvictionOrder() {
    }

    @Test
    @Disabled("LRU has not been implemented yet")
    void lruUpdatingEntryEditsMostRecent() {
    }


}
    

