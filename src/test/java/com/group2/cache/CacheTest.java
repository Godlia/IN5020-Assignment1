package com.group2.cache;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
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
 * </ul>
 * 
 * <p>Policy-independent behavior is tested against every
 * {@link EvictionPolicy} using parameterized tests.
 * 
 */
public class CacheTest {


    @Nested
    @DisplayName("Invariant checks")
    class InvariantChecks {

        @ParameterizedTest(name = "{displayName} [{0}]")
        @EnumSource(EvictionPolicy.class)
        @DisplayName("Throws exception when capacity is 0")
        void throwsWhenZeroCapacity(EvictionPolicy policy) {
            assertThrows(IllegalArgumentException.class, () -> new Cache<>(0, policy));
        }

        @ParameterizedTest(name = "{displayName} [{0}]")
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

        @ParameterizedTest(name = "{displayName} [{0}]")
        @EnumSource(EvictionPolicy.class)
        @DisplayName("Throws exception when the cache key is null in put()")
        void throwsWhenNullKeyInPut(EvictionPolicy policy) {

            Cache<String, Integer> cache = new Cache<>(5, policy);
            assertThrows(NullPointerException.class, () -> cache.put(null, 5));
        }

        @ParameterizedTest(name = "{displayName} [{0}]")
        @EnumSource(EvictionPolicy.class)
        @DisplayName("Throws exception when the cache value is null in put()")
        void throwsWhenNullValueInPut(EvictionPolicy policy) {

            Cache<String, Integer> cache = new Cache<>(5, policy);
            assertThrows(NullPointerException.class, () -> cache.put("A", null));
        }

        @ParameterizedTest(name = "{displayName} [{0}]")
        @EnumSource(EvictionPolicy.class)
        @DisplayName("Throws exception when the cache key is null in get()")
        void throwsWhenNullKeyInGet(EvictionPolicy policy) {

            Cache<String, Integer> cache = new Cache<>(5, policy);
            assertThrows(NullPointerException.class, () -> cache.get(null));
        }
    }


    @Nested
    @DisplayName("General cache behavior")
    class GeneralBehavior {

        @ParameterizedTest(name = "{displayName} [{0}]")
        @EnumSource(EvictionPolicy.class)
        @DisplayName("Can store a value and return it")
        void storesAndRetrievesValue(EvictionPolicy policy) {

            Cache<String, Integer> cache = new Cache<>(5, policy);

            cache.put("A", 1);
            Integer result = cache.get("A");

            assertEquals(1, result);
        }

        @ParameterizedTest(name = "{displayName} [{0}]")
        @EnumSource(EvictionPolicy.class)
        @DisplayName("Will return null if cache miss") 
        void returnsNullForCacheMiss(EvictionPolicy policy) {

            Cache<String, Integer> cache = new Cache<>(5, policy);
            assertNull(cache.get("A"));
        }

        @ParameterizedTest(name = "{displayName} [{0}]")
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

        @ParameterizedTest(name = "{displayName} [{0}]")
        @EnumSource(EvictionPolicy.class)
        @DisplayName("put() updates an entry and does not create an another entry")
        void putUpdatesExistingValue(EvictionPolicy policy) {

            Cache<String, Integer> cache = new Cache<>(5, policy);

            cache.put("A", 1);
            cache.put("B", 2);
            cache.put("C", 3);

            Integer initialSize = cache.size();

            // updating an existing element should not create another element
            cache.put("B", 5);

            Integer modifiedSize = cache.size();
            Integer modifiedValue = cache.get("B");

            assertAll("put update properties", 
                () -> assertEquals(initialSize, modifiedSize),
                () -> assertEquals(5, modifiedValue)
            );
        }

        @ParameterizedTest(name = "{displayName} [{0}]")
        @EnumSource(EvictionPolicy.class)
        @DisplayName("The cache must not evict before reaching capacity") 
        void doesNotEvictBeforeCapacityIsFull(EvictionPolicy policy){

            Cache<String, Integer> cache = new Cache<>(3, policy);

            cache.put("A", 1);
            cache.put("B", 2);
            cache.put("C", 3);

            assertAll("all entries remain",
                () -> assertEquals(3, cache.size()),
                () -> assertEquals(1, cache.get("A")),
                () -> assertEquals(2, cache.get("B")),
                () -> assertEquals(3, cache.get("C"))
            );
        }

        @ParameterizedTest(name = "{displayName} [{0}]")
        @EnumSource(EvictionPolicy.class)
        @DisplayName("Updating an entry does not start eviction")
        void updatingEntryDoesNotCauseEviction(EvictionPolicy policy){

            Cache<String, Integer> cache = new Cache<>(3, policy);

            cache.put("A", 1);
            cache.put("B", 2);
            cache.put("C", 3);

            cache.put("B", 5);

            assertAll("all entries remain after update",
                () -> assertEquals(3, cache.size()),
                () -> assertEquals(1, cache.get("A")),
                () -> assertEquals(5, cache.get("B")),
                () -> assertEquals(3, cache.get("C"))
            );
            
        }

        @ParameterizedTest(name = "{displayName} [{0}]")
        @EnumSource(EvictionPolicy.class)
        @DisplayName("Repeated evictions preserve the capacity invariant") 
        void continuesRespectingCapacityAfterMultipleEvictions(EvictionPolicy policy) {

            Cache<String, Integer> cache = new Cache<>(3, policy);

            cache.put("A", 1);
            cache.put("B", 2);
            cache.put("C", 3);

            cache.put("D", 4);
            assertEquals(3, cache.size());

            cache.put("E", 5);
            assertEquals(3, cache.size());

            cache.put("F", 6);
            assertEquals(3, cache.size());
        }
    }


    @Nested
    @DisplayName("FIFO behavior")
    class FIFOBehavior {

        private Cache<String, Integer> cache;

        @BeforeEach
        void setUp() {
            cache = new Cache<>(3, EvictionPolicy.FIFO);

            cache.put("A", 1);
            cache.put("B", 2);
            cache.put("C", 3);
        }

        @Test
        @DisplayName("FIFO is able to evict first inserted entry when full")
        void evictsFirstInsertedEntry() {

            // inserting a fourth entry to exceed capacity and trigger eviction
            cache.put("D", 4);

            assertAll(
                () -> assertEquals(3, cache.size()),
                () -> assertNull(cache.get("A")),
                () -> assertEquals(2, cache.get("B")),
                () -> assertEquals(3, cache.get("C")),
                () -> assertEquals(4, cache.get("D"))
            );
        }

        @Test
        @DisplayName("FIFO does not update order when running get()") 
        void accessDoesNotUpdateEvictionOrder() {

            // accessing A must not affect insertion order in FIFO.
            cache.get("A");

            // exceeding capacity to trigger eviction
            cache.put("D", 4);

            assertNull(cache.get("A"));
        }

        @Test
        @DisplayName("Updating an existing entry is not a new entry in FIFO")
        void updatingEntryDoesNotChangeEvictionOrder() {

            // updating A must preserve its original insertion position in FIFO
            cache.put("A", 100);

            // exceeding capacity to trigger eviction
            cache.put("D", 4);

            assertNull(cache.get("A"));
        }
    }


    @Nested
    @DisplayName("LRU behavior")
    class LRUBehavior {

        private Cache<String, Integer> cache;

        @BeforeEach
        void setUp() {
            cache = new Cache<>(3, EvictionPolicy.LRU);

            cache.put("A", 1);
            cache.put("B", 2);
            cache.put("C", 3);
        }

        @Test
        @DisplayName("LRU evicts the least recently used entry")
        void evictsLeastRecentlyUsedEntry() {

            // inserting a fourth entry to exceed capacity and trigger eviction
            cache.put("D", 4);

            assertNull(cache.get("A"));

            assertAll(
                () -> assertEquals(3, cache.size()),
                () -> assertNull(cache.get("A")),
                () -> assertEquals(2, cache.get("B")),
                () -> assertEquals(3, cache.get("C")),
                () -> assertEquals(4, cache.get("D"))
            );
        }

        @Test
        @DisplayName("Using get() on a key will update that recency")
        void accessUpdatesEvictionOrder() {

            // A becomes the most recently used entry and B is now the least recent
            cache.get("A");

            // exceeding capacity to trigger eviction
            cache.put("D", 4);

            assertNull(cache.get("B"));
        }

        @Test
        @DisplayName("Updating an existing element makes it most recently used")
        void updatingEntryMarksAsMostRecentlyUsed() {

            // updating A counts as access, making B the least recently used entry
            cache.put("A", 100);

            cache.put("D", 4);

            assertNull(cache.get("B"));
        }
    }

    @Nested
    @DisplayName("Timestamp behavior")
    class TimestampBehavior {

        @ParameterizedTest(name = "{displayName} [{0}]")
        @EnumSource(EvictionPolicy.class)
        @DisplayName("A new entry creates a timestamp")
        void newEntryHasTimestamp(EvictionPolicy policy){

            Cache<String, Integer> cache = new Cache<>(3, policy);

            Instant before = Instant.now();
            cache.put("A", 1);
            Instant after = Instant.now();

            Instant stamp = cache.lastUsed("A");

            assertAll(
                () -> assertNotNull(stamp),
                () -> assertFalse(stamp.isBefore(before)),
                () -> assertFalse(stamp.isAfter(after))
            );
        }

        @ParameterizedTest(name = "{displayName} [{0}]")
        @EnumSource(EvictionPolicy.class)
        @DisplayName("get() on an existing key updates its timestamp")
        void getUpdatesTimestamp(EvictionPolicy policy) throws InterruptedException {

            Cache<String, Integer> cache = new Cache<>(3, policy);
            cache.put("A", 1);
            Instant first = cache.lastUsed("A");
            Thread.sleep(5);
            cache.get("A");

            assertTrue(cache.lastUsed("A").isAfter(first));
        }
    }   
}
    

