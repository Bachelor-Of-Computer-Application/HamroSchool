package com.hammroschool.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

/**
 * Singleton MongoClient for the lifetime of the application.
 *
 * The connection URI is read from .env (MONGODB_URI).
 * Supports both:
 *   - Local:  mongodb://localhost:27017
 *   - Atlas:  mongodb+srv://user:pass@cluster.mongodb.net
 *
 * Call {@link #close()} on application exit.
 */
public final class MongoClientProvider {

    private static final MongoClientProvider INSTANCE = new MongoClientProvider();

    private final MongoClient   client;
    private final MongoDatabase database;

    private MongoClientProvider() {
        AppConfig cfg = AppConfig.getInstance();
        String uri    = cfg.getMongoUri();
        String dbName = cfg.getMongoDatabase();

        ConnectionString connStr = new ConnectionString(uri);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connStr)
                .build();

        this.client   = MongoClients.create(settings);
        this.database = client.getDatabase(dbName);

        System.out.println("[MongoClientProvider] Connected → database: " + dbName);
    }

    public static MongoClientProvider getInstance() { return INSTANCE; }

    /** Returns the application MongoDB database. */
    public MongoDatabase getDatabase() { return database; }

    /** Call once when the JavaFX application stops. */
    public void close() {
        client.close();
        System.out.println("[MongoClientProvider] Connection closed.");
    }
}
