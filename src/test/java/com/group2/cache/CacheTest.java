package com.group2.cache;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests the behavior of the {@link Cache}.
 * 
 * <p>The test suite verifies:
 * <ul>
 *      <li>cache invariants and invalid input handling</li>
 *      <li>behavior shared by all cache eviction policies</li>
 *      <li>FIFO-specific eviction behavior</li>
 *      <li>LRU-specific eviction behavior</li>
 * </ul></p>
 * 
 * <p>Policy-independent behavior is tested against every
 * {@link EvictionPolicy} using parameterized tests.
 * 
 */
public class CacheTest {


    @Nested
    class InvariantChecks {

        @ParameterizedTest
        @EnumSource(EvictionPolicy.class)
        @Disabled("Cache class not fully set up yet")
        void rejectsZeroCapacity(EvictionPolicy policy) {
        }

        @ParameterizedTest
        @EnumSource(EvictionPolicy.class)
        @Disabled("Cache class not fully set up yet")
        void rejectsNegativeCapacity(EvictionPolicy policy) {
        }

        @Test
        @Disabled("Cache class not fully set up yet")
        void rejectsNullEvictionPolicy(){
        }

        @ParameterizedTest
        @EnumSource(EvictionPolicy.class)
        @Disabled("Cache class not fully set up yet")
        void rejectsNullKey(EvictionPolicy policy) {
        }

        @ParameterizedTest
        @EnumSource(EvictionPolicy.class)
        @Disabled("Cache class not fully set up yet")
        void rejectsNullValue(EvictionPolicy policy) {
        }
    }


    @Nested
    class GeneralBehavior {

        @ParameterizedTest
        @EnumSource(EvictionPolicy.class)
        void storesAndRetrievesValue(EvictionPolicy policy) {

            Cache<String, Integer> cache = new Cache<>(5, policy);

            cache.put("A", 1);
            Integer result = cache.get("A");

            assertEquals(1, result);
        }

        @ParameterizedTest
        @EnumSource(EvictionPolicy.class)
        void doesNotEvictBeforeCapacityIsFull(EvictionPolicy policy){
        }

        @ParameterizedTest
        @EnumSource(EvictionPolicy.class)
        void updatingEntryDoesNotCauseEviction(EvictionPolicy policy){
        }


        // Replacing a value must not create an additional cache entry
        @ParameterizedTest
        @EnumSource(EvictionPolicy.class)
        @Disabled("Cache class not fully set up yet")
        void putUpdatesExistingValue(EvictionPolicy policy) {
        }

        @ParameterizedTest
        @EnumSource(EvictionPolicy.class)
        @Disabled("Cache class not fully set up yet")
        void handlesCapacityOfOne(EvictionPolicy policy) {
        }

        // Repeated evictions must still preserve the capacity invariant
        @ParameterizedTest
        @EnumSource(EvictionPolicy.class)
        @Disabled("Cache class not fully set up yet")
        void continuesRespectingCapacityAfterMultipleEvictions(EvictionPolicy policy) {
        }

        @ParameterizedTest
        @EnumSource(EvictionPolicy.class)
        @Disabled("Cache class not fully set up yet")
        void returnsNullForCacheMiss(EvictionPolicy policy) {
        }
    }


    @Nested
    class FIFOBehavior {

        @Test
        @Disabled("FIFO has not been implemented yet")
        void evictsFirstInsertedEntry() {
        }

        @Test
        @Disabled("FIFO has not been implemented yet")
        void accessDoesNotUpdateEvictionOrder() {
        }

        //Updating an existing entry is not a new entry in FIFO
        @Test
        @Disabled("FIFO has not been implemented yet")
        void updatingEntryDoesNotChangeEvictionOrder() {
        }
    }


    @Nested
    class LRUBehavior {

        @Test
        @Disabled("LRU has not been implemented yet")
        void evictsLeastRecentlyUsedEntry() {
        }

        @Test
        @Disabled("LRU has not been implemented yet")
        void accessUpdatesEvictionOrder() {
        }

        //Updating an existing entry will be the most recent lookup, and thus changes the structure
        @Test
        @Disabled("LRU has not been implemented yet")
        void updatingEntryMarksAsMostRecentlyUsed() {
        }
    }
}
    

