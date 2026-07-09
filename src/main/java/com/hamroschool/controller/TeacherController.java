package com.hamroschool.controller;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.hamroschool.model.auth.UserAccount;
import com.hamroschool.service.TeacherService;
import com.hamroschool.service.impl.TeacherServiceImpl;
import com.hamroschool.util.SceneSwitcher;
import com.hamroschool.util.SessionContext;
import com.hamroschool.util.Utils;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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

    /** Load the three-dot icon once and reuse it across all cell renders. */
    private static final Image THREE_DOT_ICON = new Image(
            Objects.requireNonNull(
                    TeacherController.class.getResourceAsStream(
                            "/com/hamroschool/assets/admin/teacher/three-dot.png")));

    private final TeacherService teacherService = TeacherServiceImpl.getInstance();

    // Master list that backs the FilteredList — never replaced, only mutated.
    private final ObservableList<UserAccount> allTeachers    = FXCollections.observableArrayList();
    private       FilteredList<UserAccount>   filteredTeachers;

    private int currentPage = 0;

    // ── FXML nodes ──────────────────────────────────────────────────────────

    @FXML private Label userInitialsLabel;
    @FXML private Label userNameLabel;

    @FXML private Label totalTeachersLabel;
    @FXML private Label activeTeachersLabel;

    @FXML private Label summaryLabel;
    @FXML private TextField searchField;

    @FXML private TableView<UserAccount>                teacherTable;
    @FXML private TableColumn<UserAccount, UserAccount> teacherColumn;
    @FXML private TableColumn<UserAccount, String>      usernameColumn;
    @FXML private TableColumn<UserAccount, UserAccount> actionsColumn;

    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Button logoutButton;

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        filteredTeachers = new FilteredList<>(allTeachers, a -> true);

        setupTable();
        refreshCurrentUser();

        // Update predicate on every keystroke — no new list allocation needed.
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            currentPage = 0;
            applyFilter(newVal);
        });

        // Load teacher list off the FX thread to keep the UI responsive during startup.
        Thread loader = new Thread(() -> {
            List<UserAccount> teachers = teacherService.getAllTeachers();
            Platform.runLater(() -> {
                allTeachers.setAll(teachers);
                totalTeachersLabel.setText(String.valueOf(allTeachers.size()));
                activeTeachersLabel.setText(String.valueOf(allTeachers.size()));
                currentPage = 0;
                applyFilter(searchField.getText());
            });
        }, "TeacherListLoader");
        loader.setDaemon(true);
        loader.start();
    }

    // ── Table setup ──────────────────────────────────────────────────────────

    private void setupTable() {
        // Teacher column — avatar initials + display name; reuse nodes per cell
        teacherColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        teacherColumn.setCellFactory(col -> new TableCell<>() {
            private final Label initialsLabel = new Label();
            private final Label nameLabel     = new Label();
            private final HBox  container     = new HBox(10, initialsLabel, nameLabel);
            {
                initialsLabel.setStyle(
                        "-fx-background-color: #e8e8e6; -fx-text-fill: #444444; " +
                        "-fx-font-size: 11px; -fx-font-weight: 800; " +
                        "-fx-background-radius: 999; " +
                        "-fx-min-width: 30; -fx-min-height: 30; " +
                        "-fx-pref-width: 30; -fx-pref-height: 30; " +
                        "-fx-alignment: center;");
                nameLabel.setStyle("-fx-text-fill: #222222; -fx-font-size: 13px; -fx-font-weight: 700;");
                container.setStyle("-fx-alignment: center-left;");
            }
            @Override
            protected void updateItem(UserAccount account, boolean empty) {
                super.updateItem(account, empty);
                if (empty || account == null) { setGraphic(null); return; }
                initialsLabel.setText(Utils.initials(account.getUsername()));
                nameLabel.setText(Utils.formatName(account.getUsername()));
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

        // Actions column — reuse ImageView and Button nodes; share the static icon image
        actionsColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final ImageView dotIcon = new ImageView(THREE_DOT_ICON);
            private final Button    btn     = new Button();
            {
                dotIcon.setFitHeight(16);
                dotIcon.setFitWidth(16);
                dotIcon.setPreserveRatio(true);
                btn.setGraphic(dotIcon);
                btn.setStyle(
                        "-fx-background-color: transparent; -fx-cursor: hand; " +
                        "-fx-padding: 6 8 6 8; -fx-background-radius: 999;");
            }
            @Override
            protected void updateItem(UserAccount account, boolean empty) {
                super.updateItem(account, empty);
                setGraphic(empty || account == null ? null : btn);
            }
        });

        teacherTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        teacherTable.setPlaceholder(new Label("No teachers added yet"));
        teacherTable.setStyle("-fx-background-color: transparent;");
    }

    // ── Filtering & pagination ────────────────────────────────────────────────

    private void applyFilter(String query) {
        String q = (query == null ? "" : query.trim().toLowerCase(Locale.ROOT));
        filteredTeachers.setPredicate(a ->
                q.isEmpty() || a.getUsername().toLowerCase(Locale.ROOT).contains(q));

        int totalPages = Math.max(1, (int) Math.ceil((double) filteredTeachers.size() / PAGE_SIZE));
        if (currentPage >= totalPages) currentPage = totalPages - 1;

        int from = currentPage * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, filteredTeachers.size());

        // Slice the filtered list for the current page
        List<UserAccount> page = filteredTeachers.subList(from, to);
        teacherTable.setItems(FXCollections.observableArrayList(page));

        summaryLabel.setText("Showing " + filteredTeachers.size() + " of " + allTeachers.size() + " teachers");
        updatePaginationButtons(totalPages);
    }

    private void updatePaginationButtons(int totalPages) {
        prevButton.setDisable(currentPage == 0);
        nextButton.setDisable(currentPage >= totalPages - 1);
    }

    // ── Header user badge ─────────────────────────────────────────────────────

    private void refreshCurrentUser() {
        SessionContext.getInstance().getCurrentUser().ifPresent(user -> {
            userInitialsLabel.setText(Utils.initials(user.getUsername()));
            userNameLabel.setText(user.getUsername());
        });
    }

    // ── FXML handlers ─────────────────────────────────────────────────────────

    @FXML
    private void handlePrevPage() {
        if (currentPage > 0) {
            currentPage--;
            applyFilter(searchField.getText());
        }
    }

    @FXML
    private void handleNextPage() {
        int totalPages = Math.max(1, (int) Math.ceil((double) filteredTeachers.size() / PAGE_SIZE));
        if (currentPage < totalPages - 1) {
            currentPage++;
            applyFilter(searchField.getText());
        }
    }

    @FXML
    private void handleNavDashboard() {
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/admin-view.fxml",
                "Admin Dashboard", SceneSwitcher.APP_WIDTH, SceneSwitcher.APP_HEIGHT);
    }

    @FXML
    private void handleNavAccounts() {
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/account-view.fxml",
                "Accounts", SceneSwitcher.APP_WIDTH, SceneSwitcher.APP_HEIGHT);
    }

    @FXML
    private void handleNavClasses() {
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/class-view.fxml",
                "Classes", SceneSwitcher.APP_WIDTH, SceneSwitcher.APP_HEIGHT);
    }

    @FXML
    private void handleNavStudents() {
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/student-view.fxml",
                "Students", SceneSwitcher.APP_WIDTH, SceneSwitcher.APP_HEIGHT);
    }

    @FXML
    private void handleNavSettings() {
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/settings-view.fxml",
                "Settings", SceneSwitcher.APP_WIDTH, SceneSwitcher.APP_HEIGHT);
    }

    @FXML
    private void handleLogout() {
        SessionContext.getInstance().clear();
        SceneSwitcher.clearCache(); // invalidate all cached scenes on logout
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/hello-view.fxml",
                "Hamro School", SceneSwitcher.LOGIN_WIDTH, SceneSwitcher.LOGIN_HEIGHT);
    }
}
