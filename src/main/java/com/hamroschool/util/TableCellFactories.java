package com.hamroschool.util;

import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;

/**
 * Reusable table cell factory methods to eliminate repetitive cell creation code
 */
public final class TableCellFactories {
    
    private TableCellFactories() {} // Prevent instantiation

    
    /**
     * Create a table cell with avatar (initials) + name
     * @param <T> The table row type
     * @param useAdminStyle If true, use admin avatar style (dark background)
     */
    public static <T> TableCell<T, String> avatarCell(boolean useAdminStyle) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String username, boolean empty) {
                super.updateItem(username, empty);
                if (empty || username == null) {
                    setGraphic(null);
                    return;
                }
                
                Label avatar = new Label(Utils.initials(username));
                avatar.setStyle(useAdminStyle ? UIStyles.AVATAR_ADMIN : UIStyles.AVATAR_DEFAULT);
                
                Label name = new Label(Utils.formatName(username));
                name.setStyle(UIStyles.TEXT_BOLD_SECONDARY);
                
                HBox container = new HBox(10, avatar, name);
                container.setStyle("-fx-alignment: center-left;");
                setGraphic(container);
            }
        };
    }
    

    
    /**
     * Create a simple badge cell with text
     * @param <T> The table row type
     */
    public static <T> TableCell<T, String> badgeCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String text, boolean empty) {
                super.updateItem(text, empty);
                if (empty || text == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                
                Label badge = new Label(text);
                badge.setStyle(UIStyles.BADGE_NEUTRAL);
                setGraphic(badge);
                setText(null);
            }
        };
    }
    
    /**
     * Create a badge cell with custom styling based on value
     * @param <T> The table row type
     * @param isAdminChecker Function to check if value represents admin/special status
     */
    public static <T> TableCell<T, String> roleBadgeCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                
                Label badge = new Label(role);
                boolean isAdmin = "Admin".equals(role);
                badge.setStyle(
                    "-fx-background-color: " + (isAdmin ? "#111111" : "#f4f4f5") + "; " +
                    "-fx-text-fill: " + (isAdmin ? "white" : "#111111") + "; " +
                    "-fx-padding: " + (isAdmin ? "4 12 4 12" : "5 10 5 10") + "; " +
                    "-fx-background-radius: 999; " +
                    "-fx-font-size: " + (isAdmin ? "12px" : "11px") + "; " +
                    "-fx-font-weight: 700;"
                );
                setGraphic(badge);
                setText(null);
            }
        };
    }
    
    
    /**
     * Create a plain text cell with custom styling
     * @param <T> The table row type
     * @param color Text color (hex code)
     * @param bold Whether text should be bold
     */
    public static <T> TableCell<T, String> plainTextCell(String color, boolean bold) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String text, boolean empty) {
                super.updateItem(text, empty);
                if (empty || text == null) {
                    setText(null);
                    return;
                }
                setText(text);
                setStyle("-fx-font-size: 13px; -fx-font-weight: " + (bold ? "600" : "400") 
                        + "; -fx-text-fill: " + color + ";");
            }
        };
    }
    
    /**
     * Create a centered text cell
     * @param <T> The table row type
     * @param color Text color (hex code)
     */
    public static <T> TableCell<T, String> centeredTextCell(String color) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String text, boolean empty) {
                super.updateItem(text, empty);
                if (empty || text == null) {
                    setText(null);
                    return;
                }
                setText(text);
                setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13px; -fx-alignment: center;");
            }
        };
    }

    
    /**
     * Create a percentage cell with color based on value
     * @param <T> The table row type
     */
    public static <T> TableCell<T, String> percentageCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    return;
                }
                
                try {
                    double pct = Double.parseDouble(value.replace("%", ""));
                    String color = UIStyles.getPercentageColor(pct);
                    setText(value);
                    setStyle("-fx-text-fill: " + color + "; -fx-font-weight: 700; " +
                            "-fx-font-size: 13px; -fx-alignment: center;");
                } catch (NumberFormatException e) {
                    setText(value);
                    setStyle("-fx-font-size: 13px;");
                }
            }
        };
    }
}
