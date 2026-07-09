package com.hamroschool.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class SceneSwitcher {

    public static final double APP_WIDTH    = 1280;
    public static final double APP_HEIGHT   = 860;
    public static final double LOGIN_WIDTH  = 1280;
    public static final double LOGIN_HEIGHT = 860;

    /** Cached scenes keyed by FXML path. Cleared on logout so stale data doesn't linger. */
    private static final Map<String, Scene> sceneCache = new HashMap<>();

    private SceneSwitcher() {}

    /**
     * Navigate to the given FXML view. Scenes are cached after the first load so that
     * repeated navigation (e.g. switching admin tabs) avoids re-parsing FXML and
     * re-executing initialize() DB queries every time.
     *
     * @param forceReload if true, the cached scene is discarded and rebuilt from scratch.
     *                    Use this after data-mutating operations to get fresh state.
     */
    public static void showView(Node source, String fxmlPath, String title,
                                double width, double height, boolean forceReload) {
        try {
            if (forceReload) {
                sceneCache.remove(fxmlPath);
            }

            Scene scene = sceneCache.get(fxmlPath);
            if (scene == null) {
                FXMLLoader loader = new FXMLLoader(SceneSwitcher.class.getResource(fxmlPath));
                Parent root = loader.load();
                scene = new Scene(root, width, height);
                sceneCache.put(fxmlPath, scene);
            }

            Stage stage = (Stage) source.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load view: " + fxmlPath, exception);
        }
    }

    /**
     * Navigate to the given FXML view, using the cache when available.
     */
    public static void showView(Node source, String fxmlPath, String title,
                                double width, double height) {
        showView(source, fxmlPath, title, width, height, false);
    }

    /**
     * Clear all cached scenes. Call this on logout so a new session always gets fresh views.
     */
    public static void clearCache() {
        sceneCache.clear();
    }
}
