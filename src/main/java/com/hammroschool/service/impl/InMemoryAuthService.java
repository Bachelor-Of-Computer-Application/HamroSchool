package com.hammroschool.service.impl;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import com.hammroschool.model.auth.UserAccount;
import com.hammroschool.model.auth.UserRole;
import com.hammroschool.service.AuthService;

public final class InMemoryAuthService implements AuthService {
    private static final InMemoryAuthService INSTANCE = new InMemoryAuthService();
    private static final String STORAGE_FILE_NAME = "accounts.properties";

    private final Map<String, UserAccount> accounts = new LinkedHashMap<>();
    private final Path storageFile = Paths.get(System.getProperty("user.dir"), STORAGE_FILE_NAME);

    private InMemoryAuthService() {
        loadAccounts();
        ensureDefaultAdmin();
        saveAccounts();
    }

    public static InMemoryAuthService getInstance() {
        return INSTANCE;
    }

    @Override
    public synchronized Optional<UserAccount> authenticate(String username, String password, UserRole role) {
        if (isBlank(username) || isBlank(password) || role == null) {
            return Optional.empty();
        }

        UserAccount account = accounts.get(key(username));
        if (account == null) {
            return Optional.empty();
        }

        if (!account.getPassword().equals(password) || account.getRole() != role) {
            return Optional.empty();
        }

        return Optional.of(account);
    }

    @Override
    public synchronized boolean createAccount(String username, String password, UserRole role) {
        if (isBlank(username) || isBlank(password) || role == null) {
            return false;
        }

        String accountKey = key(username);
        if (accounts.containsKey(accountKey)) {
            return false;
        }

        accounts.put(accountKey, new UserAccount(username, password, role));
        saveAccounts();
        return true;
    }

    @Override
    public synchronized List<UserAccount> getAccounts() {
        return new ArrayList<>(accounts.values());
    }

    private String key(String username) {
        return username.trim().toLowerCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void loadAccounts() {
        if (!Files.exists(storageFile)) {
            return;
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(storageFile)) {
            properties.load(reader);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load accounts from " + storageFile, exception);
        }

        for (String username : properties.stringPropertyNames()) {
            String value = properties.getProperty(username);
            if (value == null || !value.contains("|")) {
                continue;
            }

            String[] parts = value.split("\\|", 2);
            try {
                UserRole role = UserRole.valueOf(parts[0]);
                accounts.put(key(username), new UserAccount(username, parts[1], role));
            } catch (IllegalArgumentException ignored) {
                // Skip malformed records.
            }
        }
    }

    private void saveAccounts() {
        Properties properties = new Properties();
        for (UserAccount account : accounts.values()) {
            properties.setProperty(account.getUsername(), account.getRole().name() + "|" + account.getPassword());
        }

        try (Writer writer = Files.newBufferedWriter(storageFile)) {
            properties.store(writer, "Hammro School accounts");
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save accounts to " + storageFile, exception);
        }
    }

    private void ensureDefaultAdmin() {
        if (!accounts.containsKey(key("admin"))) {
            accounts.put(key("admin"), new UserAccount("admin", "admin123", UserRole.ADMIN));
        }
    }
}