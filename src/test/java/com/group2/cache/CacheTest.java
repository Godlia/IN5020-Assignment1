package com.group2.cache;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;


/**
 * Tests the behavior of the {@link Cache}.
 * 
 * <p>The test suite verifies:
 * <ul>
 *      <li>cache invariants and invalid input handling</li>
 *      <li>behavior shared by all cache eviction policies</li>
 *      <li>FIFO-specific eviction behavior</li>
 *      <li>LRU-specific eviction behavior</li>
 * </ul>
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
        @DisplayName("Throws exception when capacity is 0")
        void throwsWhenZeroCapacity(EvictionPolicy policy) {
            assertThrows(IllegalArgumentException.class, () -> new Cache<>(0, policy));
        }

        @ParameterizedTest
        @EnumSource(EvictionPolicy.class)
        @DisplayName("Throws exception when capacity is negative")
        void throwsWhenNegativeCapacity(EvictionPolicy policy) {
            assertThrows(IllegalArgumentException.class, () -> new Cache<>(-3, policy));
        }

        @Test
        @DisplayName("Throws exception when the eviction policy is null")
        void throwsWhenNullEvictionPolicy(){
            assertThrows(NullPointerException.class, () -> new Cache<>(5, null));
        }

        @ParameterizedTest
        @EnumSource(EvictionPolicy.class)
        @DisplayName("Throws exception when the cache key is null in put()")
        void throwsWhenNullKeyInPut(EvictionPolicy policy) {

            Cache<String, Integer> cache = new Cache<>(5, policy);
            assertThrows(NullPointerException.class, () -> cache.put(null, 5));
        }

        @ParameterizedTest
        @EnumSource(EvictionPolicy.class)
        @DisplayName("Throws exception when the cache value is null in put()")
        void throwsWhenNullValueInPut(EvictionPolicy policy) {

            Cache<String, Integer> cache = new Cache<>(5, policy);
            assertThrows(NullPointerException.class, () -> cache.put("A", null));
        }

        @ParameterizedTest
        @EnumSource(EvictionPolicy.class)
        @DisplayName("Throws exception when the cache key is null in get()")
        void throwsWhenNullKeyInGet(EvictionPolicy policy) {

            Cache<String, Integer> cache = new Cache<>(5, policy);
            assertThrows(NullPointerException.class, () -> cache.get(null));
        }
    }


    @Nested
    class GeneralBehavior {

        @ParameterizedTest
        @EnumSource(EvictionPolicy.class)
        @DisplayName("Can store a value and return it")
        void storesAndRetrievesValue(EvictionPolicy policy) {

            Cache<String, Integer> cache = new Cache<>(5, policy);

            cache.put("A", 1);
            Integer result = cache.get("A");

            assertEquals(1, result);
        }

        @ParameterizedTest
        @EnumSource(EvictionPolicy.class)
        @DisplayName("Will return null if cache miss") 
        void returnsNullForCacheMiss(EvictionPolicy policy) {

            Cache<String, Integer> cache = new Cache<>(5, policy);
            assertNull(cache.get("A"));
        }

        @ParameterizedTest
        @EnumSource(EvictionPolicy.class)
        @DisplayName("Inserting a second entry into a capacity-one cache evicts the first") 
        void insertingSecondEntryEvictsFirstWhenCapacityIsOne(EvictionPolicy policy) {

            Cache<String, Integer> cache = new Cache<>(1, policy);

            cache.put("A", 1);
            cache.put("B", 2);

            assertAll("cache properties",
                () -> assertEquals(1, cache.size()),
                () -> assertNull(cache.get("A")),
                () -> assertEquals(2, cache.get("B"))
            );
        }

        @ParameterizedTest
        @EnumSource(EvictionPolicy.class)
        @DisplayName("put() updates an entry and does not create an additional")
        void putUpdatesExistingValue(EvictionPolicy policy) {

            Cache<String, Integer> cache = new Cache<>(5, policy);

            cache.put("A", 1);
            cache.put("B", 2);
            cache.put("C", 3);

            int initialSize = cache.size();

            cache.put("B", 5);

            int modifiedSize = cache.size();
            int modifiedValue = cache.get("B");

            assertAll("put update properties", 
                () -> assertEquals(initialSize, modifiedSize),
                () -> assertEquals(5, modifiedValue)
            );
        }

        @ParameterizedTest
        @EnumSource(EvictionPolicy.class)
        @DisplayName("The cache must not evict before reaching capacity") 
        @Disabled("Cache class is not fully implemented yet")
        void doesNotEvictBeforeCapacityIsFull(EvictionPolicy policy){

            Cache<String, Integer> cache = new Cache<>(5, policy);




        }

        @ParameterizedTest
        @EnumSource(EvictionPolicy.class)
        @DisplayName("Updating an entry does not start eviction")
        @Disabled("Cache class is not fully implemented yet")
        void updatingEntryDoesNotCauseEviction(EvictionPolicy policy){
        }

        @ParameterizedTest
        @EnumSource(EvictionPolicy.class)
        @DisplayName("The eviction algorithm still preserves the capacity invariant") 
        @Disabled("Cache class not fully set up yet")
        void continuesRespectingCapacityAfterMultipleEvictions(EvictionPolicy policy) {
        }
    }


    @Nested
    class FIFOBehavior {

        private Cache<String, Integer> cache;

        @BeforeEach
        void setUp() {
            cache = new Cache<>(3, EvictionPolicy.FIFO);
        }

        @Test
        @DisplayName("FIFO is able to evict first inserted entry when full")
        @Disabled("FIFO has not been implemented yet")
        void evictsFirstInsertedEntry() {
        }

        @Test
        @DisplayName("FIFO does not update order when running get()") 
        @Disabled("FIFO has not been implemented yet")
        void accessDoesNotUpdateEvictionOrder() {
        }

        @Test
        @DisplayName("Updating an existing entry is not a new entry in FIFO")
        @Disabled("FIFO has not been implemented yet")
        void updatingEntryDoesNotChangeEvictionOrder() {
        }
    }


    @Nested
    class LRUBehavior {

        private Cache<String, Integer> cache;

        @BeforeEach
        void setUp() {
            cache = new Cache<>(3, EvictionPolicy.LRU);
        }

        @Test
        @DisplayName("LRU evicts the least recently used entry")
        @Disabled("LRU has not been implemented yet")
        void evictsLeastRecentlyUsedEntry() {
        }

        @Test
        @DisplayName("Using get() on a key will update that recency")
        @Disabled("LRU has not been implemented yet")
        void accessUpdatesEvictionOrder() {
        }

        //Updating an existing entry will be the most recent lookup, and thus changes the structure
        @Test
        @DisplayName("Caching an existing element updates the recency of that element")
        @Disabled("LRU has not been implemented yet")
        void updatingEntryMarksAsMostRecentlyUsed() {
        }
    }
}
    

