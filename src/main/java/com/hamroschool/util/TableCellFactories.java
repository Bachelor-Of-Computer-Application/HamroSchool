package com.hamroschool.util;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

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
     * Create a table cell with avatar (initials) + name + email
     * @param <T> The table row type
     */
    public static <T> TableCell<T, String> avatarWithEmailCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String username, boolean empty) {
                super.updateItem(username, empty);
                if (empty || username == null) {
                    setGraphic(null);
                    return;
                }
                
                Label avatar = new Label(Utils.initials(username));
                avatar.setStyle("-fx-background-color: #e8e8e6; -fx-text-fill: #444444; " +
                    "-fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 999; " +
                    "-fx-min-width: 32; -fx-min-height: 32; -fx-pref-width: 32; -fx-pref-height: 32; " +
                    "-fx-alignment: center;");
                
                Label name = new Label(Utils.formatName(username));
                name.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #222222;");
                
                Label email = new Label(username + "@school.edu");
                email.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");
                
                VBox nameBox = new VBox(1, name, email);
                HBox box = new HBox(10, avatar, nameBox);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
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
     * Create a grade badge cell with styling
     * @param <T> The table row type
     */
    public static <T> TableCell<T, String> gradeBadgeCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String grade, boolean empty) {
                super.updateItem(grade, empty);
                if (empty || grade == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                
                Label badge = new Label(grade);
                badge.setStyle(UIStyles.BADGE_NEUTRAL);
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
    
    /**
     * Create a status badge cell based on percentage thresholds
     * @param <T> The table row type
     */
    public static <T> TableCell<T, Double> statusBadgeCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Double pct, boolean empty) {
                super.updateItem(pct, empty);
                if (empty || pct == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                
                String status;
                String bg, fg;
                
                if (pct >= 75) {
                    status = "Good";
                    bg = "#dcfce7";
                    fg = "#16a34a";
                } else if (pct >= 50) {
                    status = "At Risk";
                    bg = "#fef9c3";
                    fg = "#a16207";
                } else {
                    status = "Critical";
                    bg = "#fee2e2";
                    fg = "#dc2626";
                }
                
                Label badge = new Label(status);
                badge.setStyle(UIStyles.createBadgeStyle(bg, fg));
                setGraphic(badge);
                setText(null);
            }
        };
    }

    
    /**
     * Create a primary button cell
     * @param <T> The table row type
     * @param buttonText Text to display on button
     * @param onAction Action to perform when button is clicked (receives row item)
     */
    public static <T> TableCell<T, T> primaryButtonCell(String buttonText, 
                                                         java.util.function.Consumer<T> onAction) {
        return new TableCell<>() {
            private final Button button = new Button(buttonText);
            
            {
                button.setStyle(UIStyles.PRIMARY_BUTTON);
                button.setOnAction(e -> {
                    T item = getTableRow().getItem();
                    if (item != null) {
                        onAction.accept(item);
                    }
                });
            }
            
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : button);
            }
        };
    }
    
    /**
     * Create a danger button cell
     * @param <T> The table row type
     * @param buttonText Text to display on button
     * @param onAction Action to perform when button is clicked (receives row item)
     */
    public static <T> TableCell<T, T> dangerButtonCell(String buttonText, 
                                                        java.util.function.Consumer<T> onAction) {
        return new TableCell<>() {
            private final Button button = new Button(buttonText);
            
            {
                button.setStyle(UIStyles.DANGER_BUTTON);
                button.setOnAction(e -> {
                    T item = getTableRow().getItem();
                    if (item != null) {
                        onAction.accept(item);
                    }
                });
            }
            
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : button);
            }
        };
    }
    
    /**
     * Create an icon button cell (transparent button with icon)
     * @param <T> The table row type
     * @param iconPath Path to icon image resource
     * @param onAction Action to perform when button is clicked (receives row item)
     */
    public static <T> TableCell<T, T> iconButtonCell(String iconPath, 
                                                      java.util.function.Consumer<T> onAction) {
        return new TableCell<>() {
            private final Button button = new Button();
            
            {
                try {
                    javafx.scene.image.ImageView icon = new javafx.scene.image.ImageView(
                        new javafx.scene.image.Image(
                            java.util.Objects.requireNonNull(
                                getClass().getResourceAsStream(iconPath))));
                    icon.setFitHeight(14);
                    icon.setFitWidth(14);
                    icon.setPreserveRatio(true);
                    button.setGraphic(icon);
                } catch (Exception e) {
                    button.setText("•••");
                }
                
                button.setStyle(UIStyles.ICON_BUTTON);
                button.setOnAction(e -> {
                    T item = getTableRow().getItem();
                    if (item != null) {
                        onAction.accept(item);
                    }
                });
            }
            
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : button);
            }
        };
    }

    
    /**
     * Create a roll number cell (shows row index)
     * @param <T> The table row type
     * @param tableView The table view to get index from
     */
    public static <T> TableCell<T, T> rollNumberCell(javafx.scene.control.TableView<T> tableView) {
        return new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                
                int idx = tableView.getItems().indexOf(item);
                setText(String.format("%02d", idx + 1));
                setStyle("-fx-text-fill: #555555; -fx-font-size: 13px; -fx-alignment: center;");
            }
        };
    }
}
