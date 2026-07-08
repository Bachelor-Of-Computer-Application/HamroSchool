package com.hamroschool.controller;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.hamroschool.model.auth.UserAccount;
import com.hamroschool.model.auth.UserRole;
import com.hamroschool.model.entity.SchoolClass;
import com.hamroschool.service.AuthService;
import com.hamroschool.service.ClassService;
import com.hamroschool.service.TeacherService;
import com.hamroschool.service.impl.MongoAuthService;
import com.hamroschool.service.impl.MongoClassService;
import com.hamroschool.service.impl.TeacherServiceImpl;
import com.hamroschool.util.SceneSwitcher;
import com.hamroschool.util.SessionContext;
import com.hamroschool.util.Utils;

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
    private final AuthService authService = MongoAuthService.getInstance();
    private final TeacherService teacherService = TeacherServiceImpl.getInstance();
    private final ClassService classService = MongoClassService.getInstance();
    private final ObservableList<UserAccount> allAccounts = FXCollections.observableArrayList();

    private FilteredList<UserAccount> filteredAccounts;
    @FXML private Label welcomeLabel;

    @FXML private Label totalAccountsLabel;
    @FXML private Label teacherCountLabel;
    @FXML private Label studentCountLabel;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ChoiceBox<UserRole> roleChoiceBox;

    @FXML private VBox subjectRow;
    @FXML private TextField subjectField;
    
    @FXML private VBox classRow;
    @FXML private ChoiceBox<String> classChoiceBox;

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
        roleChoiceBox.setItems(FXCollections.observableArrayList(UserRole.TEACHER, UserRole.STUDENT));
        roleChoiceBox.getSelectionModel().select(UserRole.TEACHER);

        loadClasses();

        updateFieldVisibility(roleChoiceBox.getValue());
        roleChoiceBox.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldRole, newRole) -> updateFieldVisibility(newRole)
        );

        userColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        usernameColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getUsername()));
        roleColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getRole().getDisplayName()));
        actionsColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));

        userColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(UserAccount account, boolean empty) {
                super.updateItem(account, empty);
                if (empty || account == null) { setGraphic(null); return; }

                Label initials = new Label(Utils.initials(account.getUsername()));
                initials.setStyle("-fx-background-color: #111111; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 999; -fx-min-width: 28; -fx-min-height: 28; -fx-pref-width: 28; -fx-pref-height: 28; -fx-alignment: center;");

                Label name = new Label(Utils.formatName(account.getUsername()));
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

                ImageView icon = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/hamroschool/assets/admin/dashboard/three-dot.png"))));
                icon.setFitHeight(14);
                icon.setFitWidth(14);
                icon.setPreserveRatio(true);

                Button button = new Button();
                button.setGraphic(icon);
                button.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6 8 6 8; -fx-background-radius: 999;");
                setGraphic(button);
            }
        });

        accountTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        accountTable.setPlaceholder(new Label("No accounts available"));

        filteredAccounts = new FilteredList<>(allAccounts, account -> true);
        SortedList<UserAccount> sortedAccounts = new SortedList<>(filteredAccounts);
        sortedAccounts.comparatorProperty().bind(accountTable.comparatorProperty());
        accountTable.setItems(sortedAccounts);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> updateFilter(newValue));

        refreshView();
    }

    private void loadClasses() {
        List<SchoolClass> classes = classService.getAllClasses();
        List<String> classNames = classes.stream()
                .map(SchoolClass::getClassName)
                .sorted((a, b) -> {
                    try {
                        int numA = Integer.parseInt(a.replaceAll("\\D+", ""));
                        int numB = Integer.parseInt(b.replaceAll("\\D+", ""));
                        return Integer.compare(numA, numB);
                    } catch (NumberFormatException e) {
                        return a.compareTo(b); 
                    }
                })
                .toList();
        
        classChoiceBox.setItems(FXCollections.observableArrayList(classNames));
        if (!classNames.isEmpty()) {
            classChoiceBox.getSelectionModel().selectFirst();
        }
    }

    private void updateFieldVisibility(UserRole role) {
        boolean isTeacher = role == UserRole.TEACHER;
        boolean isStudent = role == UserRole.STUDENT;
        
        subjectRow.setVisible(isTeacher);
        subjectRow.setManaged(isTeacher);
        
        classRow.setVisible(isStudent);
        classRow.setManaged(isStudent);
    }

    @FXML
    private void handleCreateAccount() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        UserRole role = roleChoiceBox.getValue();

        if (authService.createAccount(username, password, role)) {
            String className = classChoiceBox.getValue();
            
            if (role == UserRole.TEACHER) {
                String subject = subjectField.getText().trim();
                if (!subject.isEmpty()) {
                    teacherService.saveTeacherSubject(username, subject);
                }
                
                if (className != null && !className.isEmpty()) {
                    classService.assignTeacher(className, username);
                    statusLabel.setText("Teacher account created for " + username + " — Subject: " + subject + ", Class: " + className);
                } else {
                    statusLabel.setText("Teacher account created for " + username + " — Subject: " + subject + " (no class assigned).");
                }
            } 
            else if (role == UserRole.STUDENT) {
                if (className != null && !className.isEmpty()) {
                    classService.enrollStudent(className, username);
                    statusLabel.setText("Student account created for " + username + " — Enrolled in: " + className);
                } else {
                    statusLabel.setText("Student account created for " + username + " (not enrolled in any class).");
                }
            }
            else {
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
        SceneSwitcher.showView(welcomeLabel, "/com/hamroschool/account-view.fxml", "Accounts", 1280, 860);
    }

    @FXML
    private void handleNavClasses() {
        SceneSwitcher.showView(welcomeLabel, "/com/hamroschool/class-view.fxml", "Classes", 1280, 860);
    }

    @FXML
    private void handleNavTeachers() {
        SceneSwitcher.showView(welcomeLabel, "/com/hamroschool/teacher-view.fxml", "Teachers", 1280, 860);
    }

    @FXML
    private void handleNavStudents() {
        SceneSwitcher.showView(welcomeLabel, "/com/hamroschool/student-view.fxml", "Students", 1280, 860);
    }

    @FXML
    private void handleNavSettings() {
        SceneSwitcher.showView(welcomeLabel, "/com/hamroschool/settings-view.fxml", "Settings", 1280, 860);
    }

    @FXML
    private void handleLogout() {
        SessionContext.getInstance().clear();
        SceneSwitcher.showView(welcomeLabel, "/com/hamroschool/hello-view.fxml", "Hamro School", 920, 720);
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


}