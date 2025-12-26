package com.example.store.dao;

import com.example.store.model.Product;
import com.example.store.util.DBConnectionManager;
import com.example.store.util.CacheManager;
import com.example.store.util.OptimisticLockException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ProductDAO: JDBC CRUD operations and cache integration.
 * Uses optimistic locking via `version` column.
 * 
 * Cache Strategy - ConcurrentHashMap<Integer, Product>:
 * 
 * WHY ConcurrentHashMap (not HashMap or synchronized Map):
 * - Thread-safe for concurrent servlet requests without external locking
 * - Lock-free reads for high performance under load
 * - Fine-grained locking (segment-based) for concurrent writes
 * - Better scalability than Collections.synchronizedMap()
 * 
 * Cache Read/Write Rules:
 * - READ by ID: check cache first (O(1)), on miss load from DB and populate cache
 * - READ all: load from DB (ensures complete set), refresh cache entries along the way
 * - WRITE (create): insert DB, then cache.put() - cache always reflects successful DB writes
 * - WRITE (update): optimistic-lock UPDATE in DB, on success reload and cache.put()
 * - WRITE (delete): delete from DB, then cache.remove()
 * - Invalidation: manual refreshCache() or scheduled background refresh
 * 
 * Big-O Time Complexity:
 * - findById(id) with cache hit: O(1) - ConcurrentHashMap.get()
 * - findById(id) with cache miss: O(1) DB lookup + O(1) cache.put()
 * - create(product): O(1) DB insert + O(1) cache.put()
 * - update(product, version): O(1) DB update + O(1) cache.replace()
 * - delete(id): O(1) DB delete + O(1) cache.remove()
 * - findAll(): O(n) DB scan where n = total products
 * - refreshCache(): O(n) DB scan + n × O(1) cache operations
 */
public class ProductDAO {
    private final DBConnectionManager db;
    private final CacheManager cache;

    public ProductDAO(DBConnectionManager db, CacheManager cache) {
        this.db = db;
        this.cache = cache;
    }

    private Product mapRow(ResultSet rs) throws java.sql.SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));
        p.setName(rs.getString("name"));
        p.setPrice(rs.getBigDecimal("price"));
        p.setDescription(rs.getString("description"));
        p.setVersion(rs.getInt("version"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        p.setUpdatedAt(rs.getTimestamp("updated_at"));
        return p;
    }

    /**
     * Load all products from DB. Refreshes cache entries with current DB data.
     */
    public List<Product> findAll() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT id,name,price,description,version,created_at,updated_at FROM products";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Product p = mapRow(rs);
                list.add(p);
                cache.put(p.getId(), p);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading products", e);
        }
        return list;
    }

    /**
     * Find by id: check cache first, on miss load from DB and populate cache.
     */
    public Optional<Product> findById(int id) {
        Product cached = cache.get(id);
        if (cached != null) return Optional.of(cached);

        String sql = "SELECT id,name,price,description,version,created_at,updated_at FROM products WHERE id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Product p = mapRow(rs);
                    cache.put(p.getId(), p);
                    return Optional.of(p);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error finding product by id", e);
        }
        return Optional.empty();
    }

    /**
     * Create product in DB and cache.
     * Returns generated id.
     */
    public int create(Product product) {
        String sql = "INSERT INTO products (name,price,description,version) VALUES (?,?,?,1)";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, product.getName());
            ps.setBigDecimal(2, product.getPrice());
            ps.setString(3, product.getDescription());
            int updated = ps.executeUpdate();
            if (updated == 0) throw new RuntimeException("Insert failed, no rows affected");
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    product.setId(id);
                    product.setVersion(1);
                    cache.put(id, product);
                    return id;
                } else {
                    throw new RuntimeException("Insert failed, no ID obtained");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error creating product", e);
        }
    }

    /**
     * Update product using optimistic locking. expectedVersion must match DB version.
     * On success updates cache and returns true. On version mismatch throws OptimisticLockException.
     */
    public boolean update(Product product, int expectedVersion) throws OptimisticLockException {
        String sql = "UPDATE products SET name = ?, price = ?, description = ?, version = version + 1 WHERE id = ? AND version = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, product.getName());
            ps.setBigDecimal(2, product.getPrice());
            ps.setString(3, product.getDescription());
            ps.setInt(4, product.getId());
            ps.setInt(5, expectedVersion);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new OptimisticLockException("Update failed due to version mismatch for product id=" + product.getId());
            }
            // reload updated row to get new version and timestamps
            Optional<Product> reloaded = findById(product.getId());
            reloaded.ifPresent(p -> cache.put(p.getId(), p));
            return true;
        } catch (OptimisticLockException ole) {
            throw ole;
        } catch (Exception e) {
            throw new RuntimeException("Error updating product", e);
        }
    }

    /**
     * Delete product from DB and remove from cache.
     */
    public boolean delete(int id) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                cache.remove(id);
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Error deleting product", e);
        }
    }

    public Map<Integer, Product> getCacheSnapshot() { return cache.getSnapshot(); }

    /**
     * Refresh full cache from DB (clears then repopulates).
     */
    public void refreshCache() {
        String sql = "SELECT id,name,price,description,version,created_at,updated_at FROM products";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            cache.clear();
            while (rs.next()) {
                Product p = mapRow(rs);
                cache.put(p.getId(), p);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error refreshing cache", e);
        }
    }
}
