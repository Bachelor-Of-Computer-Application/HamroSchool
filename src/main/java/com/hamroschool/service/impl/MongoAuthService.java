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

    private static final MongoAuthService INSTANCE = new MongoAuthService();

    private final MongoCollection<Document> accounts;

    private MongoAuthService() {
        accounts = MongoClientProvider.getInstance()
                .getDatabase()
                .getCollection("user_accounts");

        accounts.createIndex(
                Indexes.ascending("username"),
                new IndexOptions().unique(true));

        createDefaultAdmin();
    }

    /** Returns the single shared instance. */
    public static MongoAuthService getInstance() {
        return INSTANCE;
    }


    @Override
    public synchronized Optional<UserAccount> authenticate(String username,
                                                           String password,
                                                           UserRole role) {
        if (isBlank(username) || isBlank(password) || role == null) {
            return Optional.empty();
        }

        Document found = accounts.find(Filters.and(
                Filters.eq("username", normalize(username)),
                Filters.eq("password", password),
                Filters.eq("role",     role.name())
        )).first();

        return found == null ? Optional.empty() : Optional.of(toUserAccount(found));
    }


    @Override
    public synchronized boolean createAccount(String username,
                                              String password,
                                              UserRole role) {
        if (isBlank(username) || isBlank(password) || role == null) return false;

        String normalUsername = normalize(username);
        if (accounts.find(Filters.eq("username", normalUsername)).first() != null) return false;

        accounts.insertOne(new Document("username", normalUsername)
                .append("password", password)
                .append("role",     role.name()));
        return true;
    }


    @Override
    public synchronized boolean createFullAccount(UserAccount account) {
        if (account == null || isBlank(account.getUsername()) || isBlank(account.getPassword())) return false;

        String normalUsername = normalize(account.getUsername());
        if (accounts.find(Filters.eq("username", normalUsername)).first() != null) return false;

        Document doc = new Document("username",         normalUsername)
                .append("password",         account.getPassword())
                .append("role",             account.getRole().name())
                .append("fullName",         account.getFullName())
                .append("gender",           account.getGender())
                .append("dateOfBirth",      account.getDateOfBirth())
                .append("phone",            account.getPhone())
                .append("email",            account.getEmail())
                .append("address",          account.getAddress())
                .append("userId",           account.getUserId())
                .append("assignedClass",    account.getAssignedClass())
                .append("rollNumber",       account.getRollNumber())
                .append("academicSession",  account.getAcademicSession())
                .append("guardianName",     account.getGuardianName())
                .append("guardianRelation", account.getGuardianRelation())
                .append("guardianPhone",    account.getGuardianPhone())
                .append("guardianEmail",    account.getGuardianEmail())
                .append("subject",          account.getSubject())
                .append("qualification",    account.getQualification())
                .append("employmentStatus", account.getEmploymentStatus());

        accounts.insertOne(doc);
        return true;
    }


    @Override
    public synchronized List<UserAccount> getAccounts() {
        List<UserAccount> result = new ArrayList<>();
        for (Document doc : accounts.find().sort(new Document("username", 1))) {
            result.add(toUserAccount(doc));
        }
        return result;
    }


    @Override
    public synchronized List<UserAccount> getAllUsersByRole(UserRole role) {
        List<UserAccount> result = new ArrayList<>();
        for (Document doc : accounts.find(Filters.eq("role", role.name())).sort(new Document("username", 1))) {
            result.add(toUserAccount(doc));
        }
        return result;
    }


    @Override
    public synchronized boolean updatePassword(String username, String newPassword) {
        if (isBlank(username) || isBlank(newPassword)) return false;

        long updatedCount = accounts.updateOne(
                Filters.eq("username", normalize(username)),
                Updates.set("password", newPassword)
        ).getMatchedCount();

        return updatedCount > 0;
    }


    /** Convert a MongoDB document into a UserAccount object. */
    private UserAccount toUserAccount(Document doc) {
        return new UserAccount(
                doc.getString("username"),
                doc.getString("password"),
                UserRole.valueOf(doc.getString("role")),
                s(doc, "fullName"),
                s(doc, "gender"),
                s(doc, "dateOfBirth"),
                s(doc, "phone"),
                s(doc, "email"),
                s(doc, "address"),
                s(doc, "userId"),
                s(doc, "assignedClass"),
                s(doc, "rollNumber"),
                s(doc, "academicSession"),
                s(doc, "guardianName"),
                s(doc, "guardianRelation"),
                s(doc, "guardianPhone"),
                s(doc, "guardianEmail"),
                // teacher
                s(doc, "subject"),
                s(doc, "qualification"),
                s(doc, "employmentStatus")
        );
    }

    private String s(Document doc, String key) {
        String v = doc.getString(key);
        return v == null ? "" : v;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalize(String username) {
        return username.trim().toLowerCase();
    }

    private void createDefaultAdmin() {
        if (authenticate("admin", "admin123", UserRole.ADMIN).isEmpty()) {
            createAccount("admin", "admin123", UserRole.ADMIN);
        }
    }
}
