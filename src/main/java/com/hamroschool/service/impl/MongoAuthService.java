package com.hamroschool.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bson.Document;

import com.hamroschool.config.MongoClientProvider;
import com.hamroschool.model.auth.UserAccount;
import com.hamroschool.model.auth.UserRole;
import com.hamroschool.service.AuthService;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;

/**
 * MongoDB-backed implementation of AuthService.
 *
 * All user accounts are stored in the "user_accounts" collection.
 * Passwords are stored as plain text for simplicity in this school project.
 * In a production system you would hash passwords (e.g. BCrypt).
 *
 * A default admin account (admin / admin123) is created automatically
 * the first time the app starts if no admin exists yet.
 */
public final class MongoAuthService implements AuthService {

    // Singleton — one instance for the whole app
    private static final MongoAuthService INSTANCE = new MongoAuthService();

    // MongoDB collection that stores all user accounts
    private final MongoCollection<Document> accounts;

    private MongoAuthService() {
        // Get the database and find the collection
        accounts = MongoClientProvider.getInstance()
                .getDatabase()
                .getCollection("user_accounts");

        // Make sure usernames are always unique in the database
        accounts.createIndex(
                Indexes.ascending("username"),
                new IndexOptions().unique(true));

        // Create the default admin account on first run
        createDefaultAdmin();
    }

    /** Returns the single shared instance. */
    public static MongoAuthService getInstance() {
        return INSTANCE;
    }

    // ── Log in ────────────────────────────────────────────────────────────────

    @Override
    public synchronized Optional<UserAccount> authenticate(String username,
                                                           String password,
                                                           UserRole role) {
        // Reject empty inputs immediately
        if (isBlank(username) || isBlank(password) || role == null) {
            return Optional.empty();
        }

        // Look for a document that matches username + password + role
        Document found = accounts.find(Filters.and(
                Filters.eq("username", normalize(username)),
                Filters.eq("password", password),
                Filters.eq("role",     role.name())
        )).first();

        // Return empty if not found, otherwise wrap in UserAccount
        return found == null ? Optional.empty() : Optional.of(toUserAccount(found));
    }

    // ── Create account ────────────────────────────────────────────────────────

    @Override
    public synchronized boolean createAccount(String username,
                                              String password,
                                              UserRole role) {
        if (isBlank(username) || isBlank(password) || role == null) return false;

        String normalUsername = normalize(username);

        // Username must be unique — reject if it already exists
        if (accounts.find(Filters.eq("username", normalUsername)).first() != null) {
            return false;
        }

        // Insert the new account document
        accounts.insertOne(new Document("username", normalUsername)
                .append("password", password)
                .append("role",     role.name()));
        return true;
    }

    // ── List all accounts ─────────────────────────────────────────────────────

    @Override
    public synchronized List<UserAccount> getAccounts() {
        List<UserAccount> result = new ArrayList<>();
        // Sort alphabetically by username
        for (Document doc : accounts.find().sort(new Document("username", 1))) {
            result.add(toUserAccount(doc));
        }
        return result;
    }

    // ── Change password ───────────────────────────────────────────────────────

    @Override
    public synchronized boolean updatePassword(String username, String newPassword) {
        if (isBlank(username) || isBlank(newPassword)) return false;

        long updatedCount = accounts.updateOne(
                Filters.eq("username", normalize(username)),
                Updates.set("password", newPassword)
        ).getMatchedCount();

        return updatedCount > 0;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Convert a MongoDB document into a UserAccount object. */
    private UserAccount toUserAccount(Document doc) {
        return new UserAccount(
                doc.getString("username"),
                doc.getString("password"),
                UserRole.valueOf(doc.getString("role")));
    }

    /** True if the string is null or contains only whitespace. */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** Lowercase and trim — all usernames are stored this way. */
    private String normalize(String username) {
        return username.trim().toLowerCase();
    }

    /** Create admin/admin123 on first run if it doesn't exist yet. */
    private void createDefaultAdmin() {
        if (authenticate("admin", "admin123", UserRole.ADMIN).isEmpty()) {
            createAccount("admin", "admin123", UserRole.ADMIN);
        }
    }
}
