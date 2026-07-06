package com.hamroschool.util;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.control.Button;
import javafx.scene.layout.Pane;

public class NavigationHelper {
    
    private final Map<Button, Pane> buttonToPaneMap = new HashMap<>();
    private Button activeButton = null;

    /**
     * Register a navigation button with its associated pane
     * @param button The navigation button
     * @param pane The pane to show when button is clicked
     */
    public void registerNav(Button button, Pane pane) {
        buttonToPaneMap.put(button, pane);
        
        button.setOnAction(e -> navigateTo(button));
        
        button.setStyle(UIStyles.NAV_BUTTON_INACTIVE);
    }

    /**
     * Navigate to a specific button's pane
     * @param button The button to activate
     */
    public void navigateTo(Button button) {
        if (!buttonToPaneMap.containsKey(button)) {
            return;
        }

        updateButtonStyles(button);
        
        Pane targetPane = buttonToPaneMap.get(button);
        for (Pane pane : buttonToPaneMap.values()) {
            boolean isTarget = pane == targetPane;
            pane.setVisible(isTarget);
            pane.setManaged(isTarget);
        }

        activeButton = button;
    }

    private void updateButtonStyles(Button activeBtn) {
        for (Button btn : buttonToPaneMap.keySet()) {
            if (btn == activeBtn) {
                btn.setStyle(UIStyles.NAV_BUTTON_ACTIVE);
            } else {
                btn.setStyle(UIStyles.NAV_BUTTON_INACTIVE);
            }
        }
    }

    public Button getActiveButton() {
        return activeButton;
    }

    public Pane getActivePane() {
        return activeButton != null ? buttonToPaneMap.get(activeButton) : null;
    }

    public void navigateToFirst() {
        if (!buttonToPaneMap.isEmpty()) {
            Button firstButton = buttonToPaneMap.keySet().iterator().next();
            navigateTo(firstButton);
        }
    }

    /**
     * Static helper: Apply active/inactive styles to buttons in a list
     * @param buttons All navigation buttons
     * @param activeButton The button that should be active
     */
    public static void setActiveButton(Iterable<Button> buttons, Button activeButton) {
        for (Button btn : buttons) {
            if (btn == activeButton) {
                btn.setStyle(UIStyles.NAV_BUTTON_ACTIVE);
            } else {
                btn.setStyle(UIStyles.NAV_BUTTON_INACTIVE);
            }
        }
    }

    /**
     * Static helper: Show one pane and hide all others
     * @param panes All panes
     * @param activePane The pane to show
     */
    public static void setActivePane(Iterable<? extends Pane> panes, Pane activePane) {
        for (Pane pane : panes) {
            boolean isActive = pane == activePane;
            pane.setVisible(isActive);
            pane.setManaged(isActive);
        }
    }
}
