package com.example.store.dao;

import com.example.store.model.User;
import com.example.store.util.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;

public class UserDAO {
    private final DBConnectionManager db;

    public UserDAO(DBConnectionManager db) {
        this.db = db;
    }

    private User map(ResultSet rs) throws Exception {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getBytes("password_hash"));
        u.setPasswordSalt(rs.getBytes("password_salt"));
        u.setRole(rs.getString("role"));
        u.setVersion(rs.getInt("version"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        u.setUpdatedAt(rs.getTimestamp("updated_at"));
        return u;
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error finding user by username", e);
        }
        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error finding user by email", e);
        }
        return Optional.empty();
    }

    public long create(User user) {
        String sql = "INSERT INTO users (username, email, password_hash, password_salt, role, version) VALUES (?, ?, ?, ?, ?, 1)";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setBytes(3, user.getPasswordHash());
            ps.setBytes(4, user.getPasswordSalt());
            ps.setString(5, user.getRole() == null ? "USER" : user.getRole());
            int updated = ps.executeUpdate();
            if (updated == 0) throw new RuntimeException("Insert failed, no rows affected");
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    user.setId(id);
                    return id;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error creating user", e);
        }
        throw new RuntimeException("Insert failed, no ID obtained");
    }
}
