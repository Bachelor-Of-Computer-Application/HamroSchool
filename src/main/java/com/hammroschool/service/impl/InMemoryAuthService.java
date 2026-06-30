package com.hammroschool.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bson.Document;

import com.hammroschool.config.MongoClientProvider;
import com.hammroschool.model.auth.UserAccount;
import com.hammroschool.model.auth.UserRole;
import com.hammroschool.service.AuthService;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;

public final class InMemoryAuthService implements AuthService {

    private static final InMemoryAuthService INSTANCE = new InMemoryAuthService();

    /** MongoDB collection name. */
    private static final String COL = "user_accounts";

    private final MongoCollection<Document> col;

    private InMemoryAuthService() {
        MongoDatabase db = MongoClientProvider.getInstance().getDatabase();
        this.col = db.getCollection(COL);
        // Unique index on username
        col.createIndex(Indexes.ascending("username"),
                new IndexOptions().unique(true).background(false));
        ensureDefaultAdmin();
    }

    public static InMemoryAuthService getInstance() { return INSTANCE; }

    // ── AuthService ───────────────────────────────────────────────────────────

    @Override
    public synchronized Optional<UserAccount> authenticate(String username, String password,
                                                            UserRole role) {
        if (blank(username) || blank(password) || role == null) return Optional.empty();
        Document doc = col.find(Filters.and(
                Filters.eq("username", normalize(username)),
                Filters.eq("password", password),
                Filters.eq("role",     role.name()))).first();
        return doc == null ? Optional.empty() : Optional.of(map(doc));
    }

    @Override
    public synchronized boolean createAccount(String username, String password, UserRole role) {
        if (blank(username) || blank(password) || role == null) return false;
        String norm = normalize(username);
        if (col.find(Filters.eq("username", norm)).first() != null) return false;
        col.insertOne(new Document("username", norm)
                .append("password", password)
                .append("role",     role.name()));
        return true;
    }

    @Override
    public synchronized List<UserAccount> getAccounts() {
        List<UserAccount> list = new ArrayList<>();
        for (Document d : col.find().sort(new Document("username", 1))) {
            list.add(map(d));
        }
        return list;
    }

    @Override
    public synchronized boolean updatePassword(String username, String newPassword) {
        if (blank(username) || blank(newPassword)) return false;
        String norm = normalize(username);
        long matched = col.updateOne(
                Filters.eq("username", norm),
                Updates.set("password", newPassword)).getMatchedCount();
        return matched > 0;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private UserAccount map(Document d) {
        return new UserAccount(
                d.getString("username"),
                d.getString("password"),
                UserRole.valueOf(d.getString("role")));
    }

    private boolean blank(String s)   { return s == null || s.trim().isEmpty(); }
    private String  normalize(String s) { return s.trim().toLowerCase(); }

    private void ensureDefaultAdmin() {
        if (authenticate("admin", "admin123", UserRole.ADMIN).isEmpty()) {
            createAccount("admin", "admin123", UserRole.ADMIN);
        }
    }
}
