package com.example.store.dao;

import com.example.store.model.Cart;
import com.example.store.model.CartItem;
import com.example.store.model.Product;
import com.example.store.util.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CartDAO: persist/load carts and cart_items.
 * Uses transactions and optimistic locking on carts table.
 */
public class CartDAO {
    private final DBConnectionManager db;

    public CartDAO(DBConnectionManager db) {
        this.db = db;
    }

    /**
     * Load cart by session_id. Returns null if not found.
     */
    public Cart loadCartBySessionId(String sessionId) {
        String sql = "SELECT id, session_id, user_id, status FROM carts WHERE session_id = ? AND status = 'OPEN'";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long cartId = rs.getLong("id");
                    Cart cart = new Cart(cartId);
                    cart.setSessionId(rs.getString("session_id"));
                    Long userId = rs.getLong("user_id");
                    if (!rs.wasNull()) cart.setUserId(userId);
                    loadCartItems(cart, conn);
                    return cart;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading cart by session_id", e);
        }
        return null;
    }

    /**
     * Load cart by cart ID. Returns null if not found.
     */
    public Cart loadCartById(long cartId) {
        String sql = "SELECT id, session_id, user_id, status FROM carts WHERE id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, cartId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Cart cart = new Cart(cartId);
                    cart.setSessionId(rs.getString("session_id"));
                    Long userId = rs.getLong("user_id");
                    if (!rs.wasNull()) cart.setUserId(userId);
                    loadCartItems(cart, conn);
                    return cart;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading cart by id", e);
        }
        return null;
    }

    private void loadCartItems(Cart cart, Connection conn) throws Exception {
        String sql = "SELECT ci.product_id, ci.quantity, ci.price_snapshot, " +
                     "p.id, p.name, p.price, p.description, p.version " +
                     "FROM cart_items ci JOIN products p ON ci.product_id = p.id " +
                     "WHERE ci.cart_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, cart.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Product p = new Product();
                    p.setId(rs.getInt("id"));
                    p.setName(rs.getString("name"));
                    p.setPrice(rs.getBigDecimal("price"));
                    p.setDescription(rs.getString("description"));
                    p.setVersion(rs.getInt("version"));
                    
                    CartItem item = new CartItem(p, rs.getInt("quantity"));
                    item.setPriceSnapshot(rs.getBigDecimal("price_snapshot"));
                    cart.getItemsMap().put(p.getId(), item);
                }
            }
        }
    }

    /**
     * Create new cart in DB. Returns generated cart ID.
     */
    public long createCart(Cart cart) {
        String sql = "INSERT INTO carts (session_id, user_id, status, version) VALUES (?, ?, 'OPEN', 1)";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cart.getSessionId());
            if (cart.getUserId() != null) {
                ps.setLong(2, cart.getUserId());
            } else {
                ps.setNull(2, java.sql.Types.BIGINT);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    cart.setId(id);
                    return id;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error creating cart", e);
        }
        throw new RuntimeException("Failed to create cart");
    }

    /**
     * Save cart items to DB. Uses transaction to ensure atomicity.
     * Deletes existing items and re-inserts current cart contents.
     */
    public boolean saveCart(Cart cart) {
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Delete existing cart_items
                String deleteSql = "DELETE FROM cart_items WHERE cart_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                    ps.setLong(1, cart.getId());
                    ps.executeUpdate();
                }

                // Insert current items
                String insertSql = "INSERT INTO cart_items (cart_id, product_id, quantity, price_snapshot) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    for (CartItem item : cart.getItems()) {
                        ps.setLong(1, cart.getId());
                        ps.setInt(2, item.getProduct().getId());
                        ps.setInt(3, item.getQuantity());
                        ps.setBigDecimal(4, item.getPriceSnapshot());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                // Update cart timestamp
                String updateSql = "UPDATE carts SET updated_at = CURRENT_TIMESTAMP WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setLong(1, cart.getId());
                    ps.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error saving cart", e);
        }
    }

    /**
     * Clear cart items and mark cart as cleared.
     */
    public boolean clearCart(long cartId) {
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String deleteSql = "DELETE FROM cart_items WHERE cart_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                    ps.setLong(1, cartId);
                    ps.executeUpdate();
                }

                String updateSql = "UPDATE carts SET status = 'CLEARED' WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setLong(1, cartId);
                    ps.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error clearing cart", e);
        }
    }

    /**
     * Load cart by user_id. Returns null if not found.
     */
    public Cart loadCartByUserId(Long userId) {
        String sql = "SELECT id, session_id, user_id, status FROM carts WHERE user_id = ? AND status = 'OPEN'";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long cartId = rs.getLong("id");
                    Cart cart = new Cart(cartId);
                    cart.setSessionId(rs.getString("session_id"));
                    cart.setUserId(userId);
                    loadCartItems(cart, conn);
                    return cart;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading cart by user_id", e);
        }
        return null;
    }

    /**
     * Update cart owner (user_id) in DB.
     */
    public void updateCartOwner(long cartId, Long userId) {
        String sql = "UPDATE carts SET user_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId != null) {
                ps.setLong(1, userId);
            } else {
                ps.setNull(1, java.sql.Types.BIGINT);
            }
            ps.setLong(2, cartId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error updating cart owner", e);
        }
    }
}
