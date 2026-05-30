package com.hammroschool.service.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.hammroschool.config.AppConfig;
import com.hammroschool.config.DatabaseSupport;
import com.hammroschool.model.auth.UserAccount;
import com.hammroschool.model.auth.UserRole;
import com.hammroschool.service.AuthService;

public final class InMemoryAuthService implements AuthService {
    private static final InMemoryAuthService INSTANCE = new InMemoryAuthService(AppConfig.getInstance());

    private final DatabaseSupport databaseSupport;

    InMemoryAuthService(AppConfig appConfig) {
        this(appConfig.getDatabaseUrl(), appConfig.getDatabaseUsername(), appConfig.getDatabasePassword(), appConfig.getDatabaseDriver());
    }

    InMemoryAuthService(String databaseUrl, String databaseUsername, String databasePassword, String databaseDriver) {
        this.databaseSupport = new DatabaseSupport(databaseUrl, databaseUsername, databasePassword, databaseDriver);
        databaseSupport.initializeSchemaIfNeeded();
        ensureDefaultAdmin();
    }

    public static InMemoryAuthService getInstance() {
        return INSTANCE;
    }

    @Override
    public synchronized Optional<UserAccount> authenticate(String username, String password, UserRole role) {
        if (isBlank(username) || isBlank(password) || role == null) {
            return Optional.empty();
        }

        String normalizedUsername = normalize(username);
        String sql = "SELECT username, password, role FROM user_accounts WHERE username = ? AND password = ? AND role = ?";
        try (Connection connection = databaseSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedUsername);
            statement.setString(2, password);
            statement.setString(3, role.name());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapAccount(resultSet));
                }
            }

            return Optional.empty();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to authenticate user " + normalizedUsername, exception);
        }
    }

    @Override
    public synchronized boolean createAccount(String username, String password, UserRole role) {
        if (isBlank(username) || isBlank(password) || role == null) {
            return false;
        }

        String normalizedUsername = normalize(username);
        String checkSql = "SELECT 1 FROM user_accounts WHERE username = ?";
        String insertSql = "INSERT INTO user_accounts (username, password, role) VALUES (?, ?, ?)";
        try (Connection connection = databaseSupport.openConnection();
             PreparedStatement checkStatement = connection.prepareStatement(checkSql)) {
            checkStatement.setString(1, normalizedUsername);

            try (ResultSet resultSet = checkStatement.executeQuery()) {
                if (resultSet.next()) {
                    return false;
                }
            }

            try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                insertStatement.setString(1, normalizedUsername);
                insertStatement.setString(2, password);
                insertStatement.setString(3, role.name());
                insertStatement.executeUpdate();
                return true;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to create account " + normalizedUsername, exception);
        }
    }

    @Override
    public synchronized List<UserAccount> getAccounts() {
        String sql = "SELECT username, password, role FROM user_accounts ORDER BY username";
        List<UserAccount> accounts = new ArrayList<>();
        try (Connection connection = databaseSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                accounts.add(mapAccount(resultSet));
            }
            return accounts;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load accounts", exception);
        }
    }

    private UserAccount mapAccount(ResultSet resultSet) throws SQLException {
        return new UserAccount(
                resultSet.getString("username"),
                resultSet.getString("password"),
                UserRole.valueOf(resultSet.getString("role")));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalize(String username) {
        return username.trim().toLowerCase();
    }

    private void ensureDefaultAdmin() {
        if (!authenticate("admin", "admin123", UserRole.ADMIN).isPresent()) {
            createAccount("admin", "admin123", UserRole.ADMIN);
        }
    }
}