package com.example.store.util;

import com.example.store.model.Product;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple CacheManager wrapping a ConcurrentHashMap<Integer, Product>.
 */
public class CacheManager {
    private final ConcurrentHashMap<Integer, Product> cache = new ConcurrentHashMap<>();

    public Product get(int id) { return cache.get(id); }
    public void put(int id, Product p) { cache.put(id, p); }
    public void remove(int id) { cache.remove(id); }
    public Map<Integer, Product> getSnapshot() { return Collections.unmodifiableMap(cache);
    }
    public void clear() { cache.clear(); }
}
