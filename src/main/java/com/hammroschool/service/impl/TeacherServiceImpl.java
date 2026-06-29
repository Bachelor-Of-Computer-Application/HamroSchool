package com.hammroschool.service.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.hammroschool.config.DatabaseSupport;
import com.hammroschool.model.auth.UserAccount;
import com.hammroschool.model.auth.UserRole;
import com.hammroschool.service.TeacherService;

public class TeacherServiceImpl implements TeacherService {

    private static final TeacherServiceImpl INSTANCE = new TeacherServiceImpl();
    private final InMemoryAuthService authService = InMemoryAuthService.getInstance();
    private final DatabaseSupport databaseSupport = DatabaseSupport.getInstance();

    private TeacherServiceImpl() {}

    public static TeacherServiceImpl getInstance() {
        return INSTANCE;
    }

    @Override
    public List<UserAccount> getAllTeachers() {
        return authService.getAccounts().stream()
            .filter(account -> account.getRole() == UserRole.TEACHER)
            .toList();
    }

    @Override
    public synchronized boolean saveTeacherSubject(String username, String subject) {
        if (isBlank(username) || isBlank(subject)) {
            return false;
        }

        String normalized = username.trim().toLowerCase();
        // MERGE (upsert): insert if not exists, update if already present
        String mergeSql =
            "MERGE INTO teachers (username, subject) KEY (username) VALUES (?, ?)";
        try (Connection conn = databaseSupport.openConnection();
             PreparedStatement stmt = conn.prepareStatement(mergeSql)) {
            stmt.setString(1, normalized);
            stmt.setString(2, subject.trim());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to save teacher subject for " + normalized, e);
        }
    }

    @Override
    public synchronized Optional<String> getSubject(String username) {
        if (isBlank(username)) {
            return Optional.empty();
        }

        String normalized = username.trim().toLowerCase();
        String sql = "SELECT subject FROM teachers WHERE username = ?";
        try (Connection conn = databaseSupport.openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, normalized);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("subject"));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to fetch subject for " + normalized, e);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
