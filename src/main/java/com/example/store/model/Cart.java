package com.example.store.model;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cart model - manages shopping cart items.
 * 
 * Data Structure Choice: LinkedHashMap<Integer, CartItem>
 * 
 * WHY LinkedHashMap (not HashMap):
 * - Preserves insertion order for predictable, user-friendly display
 * - Maintains O(1) operations for get/put/remove like HashMap
 * - Minimal overhead compared to HashMap (just doubly-linked list pointers)
 * 
 * WHY Map<Integer, CartItem> (not List<CartItem>):
 * - O(1) lookup by product ID vs O(n) linear search in List
 * - Built-in duplicate prevention via unique keys
 * - Direct key-based access for update/remove operations
 * - Automatic quantity merging when adding same product
 * 
 * Big-O Time Complexity Analysis:
 * - addProduct(product, qty): O(1) - uses Map.compute() for atomic update
 * - updateQuantity(productId, qty): O(1) - direct Map.get() + Map.put()
 * - removeProduct(productId): O(1) - Map.remove() by key
 * - totalQuantity(): O(n) - must iterate all n items to sum
 * - totalPrice(): O(n) - must iterate all n items and compute subtotals
 * - getItems(): O(n) - creates ArrayList view of n items
 * 
 * Space Complexity: O(n) where n is number of distinct products
 */
public class Cart {
    private long id;
    private String sessionId;
    private Long userId;
    private Map<Integer, CartItem> items = new LinkedHashMap<>();

    public Cart() {}

    public Cart(long id) { this.id = id; }

    /**
     * Add product to cart or merge quantity if already exists.
     * Time Complexity: O(1) - LinkedHashMap.compute() is constant time
     * Thread-safe via synchronized method for servlet concurrency safety.
     */
    public synchronized void addProduct(Product p, int qty) {
        items.compute(p.getId(), (k, v) -> {
            if (v == null) return new CartItem(p, qty);
            v.setQuantity(v.getQuantity() + qty);
            return v;
        });
    }

    /**
     * Update quantity for a product. Removes if qty <= 0.
     * Time Complexity: O(1) - direct Map access by key
     */
    public synchronized void updateQuantity(int productId, int qty) {
        CartItem item = items.get(productId);
        if (item == null) return;
        if (qty <= 0) items.remove(productId);
        else item.setQuantity(qty);
    }

    public synchronized void removeProduct(int productId) { items.remove(productId); }

    public synchronized void clear() { items.clear(); }

    public int totalQuantity() { return items.values().stream().mapToInt(CartItem::getQuantity).sum(); }

    public BigDecimal totalPrice() { return items.values().stream().map(CartItem::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add); }

    public Collection<CartItem> getItems() { return items.values(); }

    public Map<Integer, CartItem> getItemsMap() { return items; }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
