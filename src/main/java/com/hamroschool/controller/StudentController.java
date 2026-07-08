package com.hamroschool.controller;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.hamroschool.model.auth.UserAccount;
import com.hamroschool.model.auth.UserRole;
import com.hamroschool.service.AuthService;
import com.hamroschool.service.impl.MongoAuthService;
import com.hamroschool.util.SceneSwitcher;
import com.hamroschool.util.SessionContext;
import com.hamroschool.util.Utils;

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

public class StudentController {

    private static final int PAGE_SIZE = 7;

    private final AuthService authService = MongoAuthService.getInstance();
    private final ObservableList<UserAccount> allStudents = FXCollections.observableArrayList();

    private List<UserAccount> filteredStudents = List.of();
    private int currentPage = 0;

    // ── FXML nodes ──────────────────────────────────────────────────────────

    @FXML private Label userInitialsLabel;
    @FXML private Label userNameLabel;

    @FXML private Label totalStudentsLabel;
    @FXML private Label activeStudentsLabel;

    @FXML private Label summaryLabel;
    @FXML private TextField searchField;

    @FXML private TableView<UserAccount>                studentTable;
    @FXML private TableColumn<UserAccount, UserAccount> studentColumn;
    @FXML private TableColumn<UserAccount, String>      usernameColumn;
    @FXML private TableColumn<UserAccount, UserAccount> actionsColumn;

    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Button logoutButton;

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupTable();
        refreshCurrentUser();
        loadStudents();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            currentPage = 0;
            applyFilterAndPage(newVal);
        });
    }

    // ── Table setup ──────────────────────────────────────────────────────────

    private void setupTable() {
        // Student column — avatar initials + display name
        studentColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        studentColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(UserAccount account, boolean empty) {
                super.updateItem(account, empty);
                if (empty || account == null) { setGraphic(null); return; }

                Label initialsLabel = new Label(Utils.initials(account.getUsername()));
                initialsLabel.setStyle(
                    "-fx-background-color: #e8e8e6; -fx-text-fill: #444444; " +
                    "-fx-font-size: 11px; -fx-font-weight: 800; " +
                    "-fx-background-radius: 999; " +
                    "-fx-min-width: 30; -fx-min-height: 30; " +
                    "-fx-pref-width: 30; -fx-pref-height: 30; " +
                    "-fx-alignment: center;"
                );

                Label nameLabel = new Label(Utils.formatName(account.getUsername()));
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

        // Actions column — three-dot button
        actionsColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(UserAccount account, boolean empty) {
                super.updateItem(account, empty);
                if (empty || account == null) { setGraphic(null); return; }

                ImageView dotIcon = new ImageView(new Image(
                    Objects.requireNonNull(
                        getClass().getResourceAsStream(
                            "/com/hamroschool/assets/admin/student/three-dot.png"))));
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

        studentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        studentTable.setPlaceholder(new Label("No students added yet"));
        studentTable.setStyle("-fx-background-color: transparent;");
    }

    // ── Data loading ─────────────────────────────────────────────────────────

    private void loadStudents() {
        List<UserAccount> students = authService.getAccounts().stream()
            .filter(a -> a.getRole() == UserRole.STUDENT)
            .toList();

        allStudents.setAll(students);

        totalStudentsLabel.setText(String.valueOf(allStudents.size()));
        // All student accounts are considered active; no separate status field yet
        activeStudentsLabel.setText(String.valueOf(allStudents.size()));

        currentPage = 0;
        applyFilterAndPage(searchField.getText());
    }

    private void applyFilterAndPage(String query) {
        String q = (query == null ? "" : query.trim().toLowerCase(Locale.ROOT));

        List<UserAccount> filtered = allStudents.stream()
            .filter(a -> q.isEmpty()
                || a.getUsername().toLowerCase(Locale.ROOT).contains(q))
            .toList();

        filteredStudents = filtered;

        int totalPages = Math.max(1, (int) Math.ceil((double) filtered.size() / PAGE_SIZE));
        if (currentPage >= totalPages) currentPage = totalPages - 1;

        int from = currentPage * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, filtered.size());

        studentTable.setItems(FXCollections.observableArrayList(filtered.subList(from, to)));

        summaryLabel.setText("Showing " + filtered.size() + " of " + allStudents.size() + " students");
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
            applyFilterAndPage(searchField.getText());
        }
    }

    @FXML
    private void handleNextPage() {
        int totalPages = Math.max(1, (int) Math.ceil((double) filteredStudents.size() / PAGE_SIZE));
        if (currentPage < totalPages - 1) {
            currentPage++;
            applyFilterAndPage(searchField.getText());
        }
    }

    @FXML
    private void handleNavDashboard() {
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/admin-view.fxml", "Admin Dashboard", 1280, 860);
    }

    @FXML
    private void handleNavAccounts() {
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/account-view.fxml", "Accounts", 1280, 860);
    }

    @FXML
    private void handleNavClasses() {
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/class-view.fxml", "Classes", 1280, 860);
    }

    @FXML
    private void handleNavTeachers() {
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/teacher-view.fxml", "Teachers", 1280, 860);
    }

    @FXML
    private void handleNavSettings() {
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/settings-view.fxml", "Settings", 1280, 860);
    }

    @FXML
    private void handleLogout() {
        SessionContext.getInstance().clear();
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/hello-view.fxml", "Hamro School", 920, 720);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    // initials and formatDisplayName delegated to Utils
}
