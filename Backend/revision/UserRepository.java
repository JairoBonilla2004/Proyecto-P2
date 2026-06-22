package com.vulnerable.repository;

import com.vulnerable.model.User;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

@Repository
public class UserRepository {

    @PersistenceContext
    private EntityManager entityManager;

    // ============================================================
    // A05:2025 - Injection (SQL Injection)
    // ============================================================

    // Vulnerable: concatenación directa en JPQL
    public List<User> searchUsers(String keyword) {
        String jpql = "SELECT u FROM User u WHERE u.username LIKE '%" + keyword + "%'";
        Query query = entityManager.createQuery(jpql);
        return query.getResultList();
    }

    // Vulnerable: Native SQL con concatenación
    public List<User> findByRole(String role) {
        String sql = "SELECT * FROM users WHERE role = '" + role + "'";
        Query query = entityManager.createNativeQuery(sql, User.class);
        return query.getResultList();
    }

    // Vulnerable: Inyección en DELETE
    public void deleteUser(String userId) {
        String sql = "DELETE FROM users WHERE id = " + userId;
        entityManager.createNativeQuery(sql).executeUpdate();
    }

    // ============================================================
    // A04:2025 - Cryptographic Failures
    // Almacena contraseña en texto plano
    // ============================================================
    public void saveUser(User user) {
        entityManager.persist(user);
    }

    public User findByUsername(String username) {
        String jpql = "SELECT u FROM User u WHERE u.username = '" + username + "'";
        Query query = entityManager.createQuery(jpql);
        List<User> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }
}