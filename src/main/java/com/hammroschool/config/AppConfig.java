package com.hammroschool.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads configuration in this priority order (highest wins):
 *   1. JVM system properties  (-Dkey=value)
 *   2. Environment variables  (OS-level or .env file)
 *   3. application.properties (bundled in the classpath jar)
 *
 * The .env file is read from the working directory at startup.
 * It must NOT be committed to version control.
 */
public final class AppConfig {

    private static final AppConfig INSTANCE = new AppConfig();

    private final Properties dotEnv  = new Properties();
    private final Properties appProp = new Properties();

    private AppConfig() {
        loadDotEnv();
        loadAppProperties();
    }

    public static AppConfig getInstance() { return INSTANCE; }

    // ── Public accessors ──────────────────────────────────────────────────────

    public String getAppName() {
        return resolve("APP_NAME", "app.name", "Hammro School");
    }

    /**
     * Full MongoDB connection URI.
     * Reads MONGODB_URI from .env / environment first.
     * Falls back to mongodb.uri in application.properties.
     */
    public String getMongoUri() {
        return resolve("MONGODB_URI", "mongodb.uri", "mongodb://localhost:27017");
    }

    /**
     * MongoDB database name.
     * Reads MONGODB_DATABASE from .env / environment first.
     * Falls back to mongodb.database in application.properties.
     */
    public String getMongoDatabase() {
        return resolve("MONGODB_DATABASE", "mongodb.database", "hammroschool");
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Resolution order: JVM system property → OS env var → .env file value →
     * application.properties → hard-coded default.
     */
    private String resolve(String envKey, String propKey, String defaultValue) {
        // 1. JVM -D flag
        String sys = System.getProperty(propKey);
        if (notBlank(sys)) return sys.trim();

        // 2. Real OS environment variable
        String osEnv = System.getenv(envKey);
        if (notBlank(osEnv)) return strip(osEnv);

        // 3. .env file (loaded into dotEnv)
        String dotVal = dotEnv.getProperty(envKey);
        if (notBlank(dotVal)) return strip(dotVal);

        // 4. application.properties
        String propVal = appProp.getProperty(propKey);
        if (notBlank(propVal)) return propVal.trim();

        return defaultValue;
    }

    /** Parse PROJECT_ROOT/.env — silently skip if the file doesn't exist. */
    private void loadDotEnv() {
        File envFile = new File(System.getProperty("user.dir"), ".env");
        if (!envFile.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Skip blanks and comments
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq < 1) continue;
                String key   = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                dotEnv.setProperty(key, value);
            }
        } catch (IOException e) {
            System.err.println("[AppConfig] Could not read .env: " + e.getMessage());
        }
    }

    /** Load application.properties from classpath. */
    private void loadAppProperties() {
        try (InputStream in = AppConfig.class.getResourceAsStream("/application.properties")) {
            if (in != null) appProp.load(in);
        } catch (IOException e) {
            System.err.println("[AppConfig] Could not read application.properties: " + e.getMessage());
        }
    }

    /** Remove surrounding single or double quotes (e.g. from .env values). */
    private static String strip(String value) {
        if (value == null) return "";
        value = value.trim();
        if ((value.startsWith("\"") && value.endsWith("\""))
         || (value.startsWith("'")  && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
