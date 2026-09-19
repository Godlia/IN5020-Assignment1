package com.group2.cache;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CacheTest {

    @Test
    void storesAndRetrievesValueFIFO() {

        Cache<String, Integer> cache = new Cache<>(5, EvictionPolicy.FIFO);

        cache.put("A", 1);
        Integer result = cache.get("A");

        assertEquals(1, result);
    }

    @Test
    @Disabled("Cache class not fully set up yet")
    void doesNotExceedCapacity() {
    }

    @Test
    @Disabled("Cache class not fully set up yet")
    void fifoEvictsFirstInsertedEntry() {
    }

    @Test
    @Disabled("Cache class not fully set up yet")
    void lruEvictsLeastRecentlyUsedEntry() {
    }

    @Test
    @Disabled("Cache class not fully set up yet")
    void rejectsZeroCapacity() {
    }
}
    

