package com.hammroschool.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseSupport {
    private static final DatabaseSupport INSTANCE = new DatabaseSupport(AppConfig.getInstance());

    private final String databaseUrl;
    private final String databaseUsername;
    private final String databasePassword;
    private final String databaseDriver;
    private boolean initialized;

    DatabaseSupport(AppConfig appConfig) {
        this(appConfig.getDatabaseUrl(), appConfig.getDatabaseUsername(), appConfig.getDatabasePassword(), appConfig.getDatabaseDriver());
    }

    public DatabaseSupport(String databaseUrl, String databaseUsername, String databasePassword, String databaseDriver) {
        this.databaseUrl = databaseUrl;
        this.databaseUsername = databaseUsername;
        this.databasePassword = databasePassword;
        this.databaseDriver = databaseDriver;
    }

    public static DatabaseSupport getInstance() {
        return INSTANCE;
    }

    public synchronized Connection openConnection() throws SQLException {
        initializeSchemaIfNeeded();
        return DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword);
    }

    public synchronized void initializeSchemaIfNeeded() {
        if (initialized) {
            return;
        }

        ensureDatabaseDirectoryExists();
        try {
            Class.forName(databaseDriver);
            try (Connection connection = DriverManager.getConnection(
                databaseUrl,
                databaseUsername,
                databasePassword);
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate(loadSqlFromResource("create_user_accounts.sql"));
                statement.executeUpdate(loadSqlFromResource("create_students.sql"));
                statement.executeUpdate(loadSqlFromResource("create_teachers.sql"));
                statement.executeUpdate(loadSqlFromResource("create_class_rooms.sql"));
                statement.executeUpdate(loadSqlFromResource("create_marks.sql"));
                statement.executeUpdate(loadSqlFromResource("create_attendance.sql"));
                runMigrations(connection);
            }
            initialized = true;
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Unable to load database driver", exception);
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to initialize database schema", exception);
        }
    }

    private void runMigrations(Connection connection) throws SQLException {
        // Add username and subject columns to teachers table if they don't exist yet
        String[] migrations = {
            "ALTER TABLE teachers ADD COLUMN IF NOT EXISTS username VARCHAR(100)",
            "ALTER TABLE teachers ADD COLUMN IF NOT EXISTS subject VARCHAR(255)"
        };
        try (Statement stmt = connection.createStatement()) {
            for (String sql : migrations) {
                stmt.executeUpdate(sql);
            }
        }
    }

    private String loadSqlFromResource(String resourceName) {
        String path = "/sql/" + resourceName;
        try (InputStream in = DatabaseSupport.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("SQL resource not found: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read SQL resource: " + path, exception);
        }
    }

    private void ensureDatabaseDirectoryExists() {
        if (!databaseUrl.startsWith("jdbc:h2:file:")) {
            return;
        }

        String filePath = databaseUrl.substring("jdbc:h2:file:".length());
        int optionsStart = filePath.indexOf(';');
        if (optionsStart >= 0) {
            filePath = filePath.substring(0, optionsStart);
        }

        Path databaseFile = Paths.get(System.getProperty("user.dir"), filePath).normalize();
        Path parentDirectory = databaseFile.getParent();
        if (parentDirectory == null) {
            return;
        }

        try {
            Files.createDirectories(parentDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create database directory " + parentDirectory, exception);
        }
    }
}