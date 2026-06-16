package com.hammroschool.controller;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.hammroschool.model.entity.Teacher;
import com.hammroschool.service.TeacherService;
import com.hammroschool.service.impl.TeacherServiceImpl;
import com.hammroschool.util.SceneSwitcher;
import com.hammroschool.util.SessionContext;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

public class TeacherController {

    private static final int PAGE_SIZE = 7;

    private final TeacherService teacherService = TeacherServiceImpl.getInstance();
    private final ObservableList<Teacher> allTeachers = FXCollections.observableArrayList();

    private List<Teacher> filteredTeachers = List.of();
    private int currentPage = 0;

    // ── FXML nodes ──────────────────────────────────────────────────────────

    @FXML private Label userInitialsLabel;
    @FXML private Label userNameLabel;

    @FXML private Label totalTeachersLabel;
    @FXML private Label activeTeachersLabel;
    @FXML private Label newThisMonthLabel;

    @FXML private Label summaryLabel;
    @FXML private TextField searchField;

    @FXML private TableView<Teacher>                teacherTable;
    @FXML private TableColumn<Teacher, Teacher>     teacherColumn;
    @FXML private TableColumn<Teacher, String>      usernameColumn;
    @FXML private TableColumn<Teacher, String>      subjectColumn;
    @FXML private TableColumn<Teacher, Teacher>     statusColumn;
    @FXML private TableColumn<Teacher, Teacher>     actionsColumn;

    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Button logoutButton;

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupTable();
        refreshCurrentUser();
        loadTeachers();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            currentPage = 0;
            applyFilterAndPage(newVal);
        });
    }

    // ── Table setup ──────────────────────────────────────────────────────────

    private void setupTable() {
        // Teacher column — avatar initials + display name
        teacherColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        teacherColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Teacher teacher, boolean empty) {
                super.updateItem(teacher, empty);
                if (empty || teacher == null) { setGraphic(null); return; }

                Label initialsLabel = new Label(getInitials(teacher.getName()));
                initialsLabel.setStyle(
                    "-fx-background-color: #e8e8e6; -fx-text-fill: #444444; " +
                    "-fx-font-size: 11px; -fx-font-weight: 800; " +
                    "-fx-background-radius: 999; " +
                    "-fx-min-width: 30; -fx-min-height: 30; " +
                    "-fx-pref-width: 30; -fx-pref-height: 30; " +
                    "-fx-alignment: center;"
                );

                Label nameLabel = new Label(teacher.getName());
                nameLabel.setStyle("-fx-text-fill: #222222; -fx-font-size: 13px; -fx-font-weight: 700;");

                HBox container = new HBox(10, initialsLabel, nameLabel);
                container.setStyle("-fx-alignment: center-left;");
                setGraphic(container);
            }
        });

        // Username column — muted text
        usernameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getUsername()));
        usernameColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String username, boolean empty) {
                super.updateItem(username, empty);
                if (empty || username == null) { setText(null); return; }
                setText(username);
                setStyle("-fx-text-fill: #888888; -fx-font-size: 13px;");
            }
        });

        // Subject column
        subjectColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getSubject()));
        subjectColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String subject, boolean empty) {
                super.updateItem(subject, empty);
                if (empty || subject == null) { setText(null); return; }
                setText(subject);
                setStyle("-fx-text-fill: #222222; -fx-font-size: 13px;");
            }
        });

        // Status column — pill badge
        statusColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Teacher teacher, boolean empty) {
                super.updateItem(teacher, empty);
                if (empty || teacher == null) { setGraphic(null); setText(null); return; }

                Label badge = new Label(teacher.isActive() ? "Active" : "Inactive");
                if (teacher.isActive()) {
                    badge.setStyle(
                        "-fx-background-color: #dcfce7; -fx-text-fill: #166534; " +
                        "-fx-padding: 4 12 4 12; -fx-background-radius: 999; " +
                        "-fx-font-size: 12px; -fx-font-weight: 700;"
                    );
                } else {
                    badge.setStyle(
                        "-fx-background-color: #f4f4f5; -fx-text-fill: #71717a; " +
                        "-fx-padding: 4 12 4 12; -fx-background-radius: 999; " +
                        "-fx-font-size: 12px; -fx-font-weight: 700;"
                    );
                }
                setGraphic(badge);
                setText(null);
            }
        });

        // Actions column — three-dot button
        actionsColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Teacher teacher, boolean empty) {
                super.updateItem(teacher, empty);
                if (empty || teacher == null) { setGraphic(null); return; }

                ImageView dotIcon = new ImageView(new Image(
                    Objects.requireNonNull(
                        getClass().getResourceAsStream(
                            "/com/hammroschool/assets/admin/teacher/three-dot.png"))));
                dotIcon.setFitHeight(16);
                dotIcon.setFitWidth(16);
                dotIcon.setPreserveRatio(true);

                Button btn = new Button();
                btn.setGraphic(dotIcon);
                btn.setStyle(
                    "-fx-background-color: transparent; -fx-cursor: hand; " +
                    "-fx-padding: 6 8 6 8; -fx-background-radius: 999;"
                );
                setGraphic(btn);
            }
        });

        teacherTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        teacherTable.setPlaceholder(new Label("No teachers found"));
        teacherTable.setStyle("-fx-background-color: transparent;");
    }

    // ── Data loading ─────────────────────────────────────────────────────────

    private void loadTeachers() {
        allTeachers.setAll(teacherService.getAllTeachers());

        totalTeachersLabel.setText(String.valueOf(allTeachers.size()));
        activeTeachersLabel.setText(String.valueOf(teacherService.countActive()));
        newThisMonthLabel.setText(String.valueOf(teacherService.countNewThisMonth()));

        currentPage = 0;
        applyFilterAndPage(searchField.getText());
    }

    private void applyFilterAndPage(String query) {
        String q = (query == null ? "" : query.trim().toLowerCase(Locale.ROOT));

        List<Teacher> filtered = allTeachers.stream()
            .filter(t -> q.isEmpty()
                || t.getName().toLowerCase(Locale.ROOT).contains(q)
                || t.getUsername().toLowerCase(Locale.ROOT).contains(q)
                || t.getSubject().toLowerCase(Locale.ROOT).contains(q))
            .toList();

        filteredTeachers = filtered;

        int totalPages = Math.max(1, (int) Math.ceil((double) filtered.size() / PAGE_SIZE));
        if (currentPage >= totalPages) currentPage = totalPages - 1;

        int from = currentPage * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, filtered.size());

        teacherTable.setItems(FXCollections.observableArrayList(filtered.subList(from, to)));

        summaryLabel.setText("Showing " + filtered.size() + " of " + allTeachers.size() + " teachers");
        updatePaginationButtons(totalPages);
    }

    private void updatePaginationButtons(int totalPages) {
        prevButton.setDisable(currentPage == 0);
        nextButton.setDisable(currentPage >= totalPages - 1);
    }

    // ── Header user badge ─────────────────────────────────────────────────────

    private void refreshCurrentUser() {
        SessionContext.getInstance().getCurrentUser().ifPresent(user -> {
            userInitialsLabel.setText(getInitials(user.getUsername()));
            userNameLabel.setText(user.getUsername());
        });
    }

    // ── FXML handlers ─────────────────────────────────────────────────────────

    @FXML
    private void handlePrevPage() {
        if (currentPage > 0) {
            currentPage--;
            applyFilterAndPage(searchField.getText());
        }
    }

    @FXML
    private void handleNextPage() {
        int totalPages = Math.max(1, (int) Math.ceil((double) filteredTeachers.size() / PAGE_SIZE));
        if (currentPage < totalPages - 1) {
            currentPage++;
            applyFilterAndPage(searchField.getText());
        }
    }

    @FXML
    private void handleNavDashboard() {
        SceneSwitcher.showView(logoutButton, "/com/hammroschool/admin-view.fxml", "Admin Dashboard", 1280, 860);
    }

    @FXML
    private void handleNavAccounts() {
        SceneSwitcher.showView(logoutButton, "/com/hammroschool/account-view.fxml", "Accounts", 1280, 860);
    }

    @FXML
    private void handleLogout() {
        SessionContext.getInstance().clear();
        SceneSwitcher.showView(logoutButton, "/com/hammroschool/hello-view.fxml", "Hammro School", 920, 720);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }
}
