package com.group2.cache;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CacheTest {

    // ***** Invariant checks *****

    @Test
    @Disabled("Cache class not fully set up yet")
    void rejectsZeroCapacity() {
    }

    @Test
    @Disabled("Cache class not fully set up yet")
    void rejectsNegativeCapacity() {
    }

    @Test
    @Disabled("Cache class not fully set up yet")
    void rejectsNullEvictionPolicy(){
    }

    @Test
    @Disabled("Cache class not fully set up yet")
    void rejectsNullKey() {
    }

    @Test
    @Disabled("Cache class not fully set up yet")
    void rejectsNullValue() {
    }


    // ***** General behavior *****

    @Test
    void storesAndRetrievesValueFIFO() {

        Cache<String, Integer> cache = new Cache<>(5, EvictionPolicy.FIFO);

        cache.put("A", 1);
        Integer result = cache.get("A");

        assertEquals(1, result);
    }

    @Test
    @Disabled("Cache class not fully set up yet")
    void storesAndRetrievesValueLRU() {
    }    
    

    // What happens if the same key is inserted twice?
    // Why test this? Otherwise, the second put() could be treated as a brand-new cache entry. One key should represent one mapping.
    // assertEquals(1, cache.size());
    @Test
    @Disabled("Cache class not fully set up yet")
    void putUpdatesExistingValue() {
    }


    // Edge case test
    // Checking that the cache is able to handle a cache with capacity of one
    // Boundary values often exposes bugs
    @Test
    @Disabled("Cache class not fully set up yet")
    void handlesCapacityOfOne() {
    }


    // 
    @Test
    @Disabled("Cache class not fully set up yet")
    void continuesRespectingCapacityAfterMultipleEvictions() {
    }

    @Test
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
    

