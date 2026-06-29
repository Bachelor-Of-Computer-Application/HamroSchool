package com.hammroschool.controller;

import java.util.Locale;
import java.util.Objects;

import com.hammroschool.model.auth.UserAccount;
import com.hammroschool.model.auth.UserRole;
import com.hammroschool.service.AuthService;
import com.hammroschool.service.TeacherService;
import com.hammroschool.service.impl.InMemoryAuthService;
import com.hammroschool.service.impl.TeacherServiceImpl;
import com.hammroschool.util.SceneSwitcher;
import com.hammroschool.util.SessionContext;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class AdminController {
    private final AuthService authService = InMemoryAuthService.getInstance();
    private final TeacherService teacherService = TeacherServiceImpl.getInstance();
    private final ObservableList<UserAccount> allAccounts = FXCollections.observableArrayList();

    private FilteredList<UserAccount> filteredAccounts;
    @FXML private Label welcomeLabel;

    @FXML private Label totalAccountsLabel;
    @FXML private Label teacherCountLabel;
    @FXML private Label studentCountLabel;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ChoiceBox<UserRole> roleChoiceBox;

    /** Container VBox for the subject row — shown only when TEACHER role is selected */
    @FXML private VBox subjectRow;
    @FXML private TextField subjectField;

    @FXML private Label statusLabel;

    @FXML private TextField searchField;
    @FXML private Label summaryLabel;

    @FXML private TableView<UserAccount> accountTable;
    @FXML private TableColumn<UserAccount, UserAccount> userColumn;
    @FXML private TableColumn<UserAccount, String> usernameColumn;
    @FXML private TableColumn<UserAccount, String> roleColumn;
    @FXML private TableColumn<UserAccount, UserAccount> actionsColumn;

    @FXML
    public void initialize() {
        // Role choice box
        roleChoiceBox.setItems(FXCollections.observableArrayList(UserRole.values()));
        roleChoiceBox.getSelectionModel().select(UserRole.TEACHER);

        // Subject field — shown only when TEACHER is selected
        updateSubjectRowVisibility(roleChoiceBox.getValue());
        roleChoiceBox.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldRole, newRole) -> updateSubjectRowVisibility(newRole)
        );

        // Table columns
        userColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        usernameColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getUsername()));
        roleColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getRole().getDisplayName()));
        actionsColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));

        userColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(UserAccount account, boolean empty) {
                super.updateItem(account, empty);
                if (empty || account == null) { setGraphic(null); return; }

                Label initials = new Label(getInitials(account.getUsername()));
                initials.setStyle("-fx-background-color: #111111; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 999; -fx-min-width: 28; -fx-min-height: 28; -fx-pref-width: 28; -fx-pref-height: 28; -fx-alignment: center;");

                Label name = new Label(formatDisplayName(account.getUsername()));
                name.setStyle("-fx-text-fill: #222222; -fx-font-size: 13px; -fx-font-weight: 700;");

                HBox container = new HBox(10, initials, name);
                container.setStyle("-fx-alignment: center-left;");
                setGraphic(container);
            }
        });

        roleColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) { setGraphic(null); setText(null); return; }

                Label badge = new Label(role);
                badge.setStyle("-fx-background-color: #f4f4f5; -fx-text-fill: #111111; -fx-padding: 5 10 5 10; -fx-background-radius: 999; -fx-font-size: 11px; -fx-font-weight: 700;");
                setGraphic(badge);
                setText(null);
            }
        });

        actionsColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(UserAccount account, boolean empty) {
                super.updateItem(account, empty);
                if (empty || account == null) { setGraphic(null); return; }

                ImageView icon = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/hammroschool/images/admin-dashboard/three-dot-icon.png"))));
                icon.setFitHeight(14);
                icon.setFitWidth(14);
                icon.setPreserveRatio(true);

                Button button = new Button();
                button.setGraphic(icon);
                button.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6 8 6 8; -fx-background-radius: 999;");
                setGraphic(button);
            }
        });

        accountTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        accountTable.setPlaceholder(new Label("No accounts available"));

        filteredAccounts = new FilteredList<>(allAccounts, account -> true);
        SortedList<UserAccount> sortedAccounts = new SortedList<>(filteredAccounts);
        sortedAccounts.comparatorProperty().bind(accountTable.comparatorProperty());
        accountTable.setItems(sortedAccounts);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> updateFilter(newValue));

        refreshView();
    }

    private void updateSubjectRowVisibility(UserRole role) {
        boolean isTeacher = role == UserRole.TEACHER;
        subjectRow.setVisible(isTeacher);
        subjectRow.setManaged(isTeacher);
    }

    @FXML
    private void handleCreateAccount() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        UserRole role = roleChoiceBox.getValue();

        if (authService.createAccount(username, password, role)) {
            // If teacher, also save the typed subject
            if (role == UserRole.TEACHER) {
                String subject = subjectField.getText().trim();
                if (!subject.isEmpty()) {
                    teacherService.saveTeacherSubject(username, subject);
                    statusLabel.setText("Teacher account created for " + username + " — Subject: " + subject + ".");
                } else {
                    statusLabel.setText("Teacher account created for " + username + " (no subject assigned).");
                }
            } else {
                statusLabel.setText("Account created for " + username + " as " + role.getDisplayName() + ".");
            }
            usernameField.clear();
            passwordField.clear();
            subjectField.clear();
            roleChoiceBox.getSelectionModel().select(UserRole.TEACHER);
            refreshView();
        } else {
            statusLabel.setText("Username is required, password is required, and the username must be unique.");
        }
    }

    @FXML
    private void handleNavAccounts() {
        SceneSwitcher.showView(welcomeLabel, "/com/hammroschool/account-view.fxml", "Accounts", 1280, 860);
    }

    @FXML
    private void handleNavTeachers() {
        SceneSwitcher.showView(welcomeLabel, "/com/hammroschool/teacher-view.fxml", "Teachers", 1280, 860);
    }

    @FXML
    private void handleNavStudents() {
        SceneSwitcher.showView(welcomeLabel, "/com/hammroschool/student-view.fxml", "Students", 1280, 860);
    }

    @FXML
    private void handleNavSettings() {
        SceneSwitcher.showView(welcomeLabel, "/com/hammroschool/settings-view.fxml", "Settings", 1280, 860);
    }

    @FXML
    private void handleLogout() {
        SessionContext.getInstance().clear();
        SceneSwitcher.showView(welcomeLabel, "/com/hammroschool/hello-view.fxml", "Hammro School", 920, 720);
    }

    private void refreshView() {
        UserAccount currentUser = SessionContext.getInstance().getCurrentUser().orElseThrow();
        welcomeLabel.setText("Welcome, " + currentUser.getUsername());

        allAccounts.setAll(authService.getAccounts());
        updateSummary();
        updateFilter(searchField.getText());
        statusLabel.setText("");
    }

    private void updateSummary() {
        long totalAccounts = allAccounts.size();
        long teacherAccounts = allAccounts.stream().filter(a -> a.getRole() == UserRole.TEACHER).count();
        long studentAccounts = allAccounts.stream().filter(a -> a.getRole() == UserRole.STUDENT).count();

        totalAccountsLabel.setText(Long.toString(totalAccounts));
        teacherCountLabel.setText(Long.toString(teacherAccounts));
        studentCountLabel.setText(Long.toString(studentAccounts));
        summaryLabel.setText("Showing " + totalAccounts + " of " + totalAccounts + " accounts");
    }

    private void updateFilter(String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        filteredAccounts.setPredicate(account -> {
            if (normalizedQuery.isEmpty()) return true;
            return account.getUsername().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    || account.getRole().getDisplayName().toLowerCase(Locale.ROOT).contains(normalizedQuery);
        });
        summaryLabel.setText("Showing " + filteredAccounts.size() + " of " + allAccounts.size() + " accounts");
    }

    private String getInitials(String username) {
        if (username == null || username.isBlank()) return "?";
        String[] parts = username.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private String formatDisplayName(String username) {
        if (username == null || username.isBlank()) return "Unknown user";
        String trimmed = username.trim();
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
    }
}