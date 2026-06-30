package com.hamroschool.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bson.Document;

import com.hamroschool.config.MongoClientProvider;
import com.hamroschool.model.auth.UserAccount;
import com.hamroschool.model.auth.UserRole;
import com.hamroschool.service.TeacherService;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;

public class TeacherServiceImpl implements TeacherService {

    private static final TeacherServiceImpl INSTANCE = new TeacherServiceImpl();

    private final MongoCollection<Document> col;

    private TeacherServiceImpl() {
        MongoDatabase db = MongoClientProvider.getInstance().getDatabase();
        col = db.getCollection("user_accounts");
    }

    public static TeacherServiceImpl getInstance() { return INSTANCE; }

    @Override
    public List<UserAccount> getAllTeachers() {
        List<UserAccount> list = new ArrayList<>();
        for (Document d : col.find(Filters.eq("role", UserRole.TEACHER.name()))
                             .sort(Sorts.ascending("username"))) {
            list.add(new UserAccount(
                    d.getString("username"),
                    d.getString("password"),
                    UserRole.TEACHER));
        }
        return list;
    }

    @Override
    public boolean saveTeacherSubject(String username, String subject) {
        if (username == null || username.isBlank()) return false;
        long matched = col.updateOne(
                Filters.eq("username", username.trim().toLowerCase()),
                Updates.set("subject", subject == null ? "" : subject.trim()))
                .getMatchedCount();
        return matched > 0;
    }

    @Override
    public Optional<String> getSubject(String username) {
        if (username == null || username.isBlank()) return Optional.empty();
        Document d = col.find(Filters.eq("username", username.trim().toLowerCase())).first();
        if (d == null) return Optional.empty();
        String subject = d.getString("subject");
        return (subject != null && !subject.isBlank()) ? Optional.of(subject) : Optional.empty();
    }
}
