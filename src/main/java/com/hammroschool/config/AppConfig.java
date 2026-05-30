package com.hammroschool.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {
    private static final AppConfig INSTANCE = new AppConfig();

    private final Properties properties = new Properties();

    private AppConfig() {
        try (InputStream inputStream = AppConfig.class.getResourceAsStream("/application.properties")) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load application.properties", exception);
        }
    }

    public static AppConfig getInstance() {
        return INSTANCE;
    }

    public String getAppName() {
        return valueOf("app.name", "Hammro School");
    }

    public String getDatabaseUrl() {
        return valueOf("db.url", "jdbc:h2:file:./data/hammroschool");
    }

    public String getDatabaseUsername() {
        return valueOf("db.username", "sa");
    }

    public String getDatabasePassword() {
        return valueOf("db.password", "");
    }

    public String getDatabaseDriver() {
        return valueOf("db.driver", "org.h2.Driver");
    }

    private String valueOf(String key, String defaultValue) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.trim().isEmpty()) {
            return systemValue.trim();
        }

        String propertyValue = properties.getProperty(key);
        if (propertyValue != null && !propertyValue.trim().isEmpty()) {
            return propertyValue.trim();
        }

        return defaultValue;
    }
}
