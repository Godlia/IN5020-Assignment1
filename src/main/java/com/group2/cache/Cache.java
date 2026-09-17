package com.group2.cache;

import java.util.LinkedHashMap;
import java.util.Map;

public class Cache {
    private LinkedHashMap<String, Integer> cache = new LinkedHashMap<>(){
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
            return size() > 150;
        }
    };
}
