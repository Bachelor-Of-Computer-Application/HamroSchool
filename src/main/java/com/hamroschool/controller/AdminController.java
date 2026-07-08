package com.hamroschool.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import javafx.fxml.FXML;
import javafx.geometry.Pos;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class AdminController {

    private final AuthService    authService    = MongoAuthService.getInstance();
    private final TeacherService teacherService = TeacherServiceImpl.getInstance();
    private final ClassService   classService   = MongoClassService.getInstance();

    private final ObservableList<UserAccount> allAccounts = FXCollections.observableArrayList();
    private FilteredList<UserAccount> filteredAccounts;

    private static final int PAGE_SIZE = 8;
    private int currentPage = 0;
    private UserRole selectedRole = UserRole.STUDENT;

    // ── Header ────────────────────────────────────────────────────────────────
    @FXML private Label welcomeLabel;
    @FXML private Label userInitialsLabel;
    @FXML private Label userNameLabel;

    // ── Stats ─────────────────────────────────────────────────────────────────
    @FXML private Label totalAccountsLabel;
    @FXML private Label teacherCountLabel;
    @FXML private Label studentCountLabel;

    // ── Table toolbar ─────────────────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private Button    filterRoleBtn;
    @FXML private Button    filterStatusBtn;
    @FXML private Button    sortBtn;
    @FXML private HBox      bulkActionBar;
    @FXML private Label     selectedCountLabel;

    // ── Table ─────────────────────────────────────────────────────────────────
    @FXML private TableView<UserAccount>                accountTable;
    @FXML private TableColumn<UserAccount, UserAccount> selectColumn;
    @FXML private TableColumn<UserAccount, UserAccount> userColumn;
    @FXML private TableColumn<UserAccount, String>      usernameColumn;
    @FXML private TableColumn<UserAccount, String>      roleColumn;
    @FXML private TableColumn<UserAccount, String>      phoneColumn;
    @FXML private TableColumn<UserAccount, String>      emailColumn;
    @FXML private TableColumn<UserAccount, String>      statusColumn;
    @FXML private TableColumn<UserAccount, String>      createdColumn;
    @FXML private TableColumn<UserAccount, UserAccount> actionsColumn;
    @FXML private Label  summaryLabel;
    @FXML private Button prevButton;
    @FXML private Button nextButton;

    // ── Modal skeleton ────────────────────────────────────────────────────────
    @FXML private StackPane modalOverlay;
    @FXML private VBox      step1Pane;
    @FXML private VBox      step2TeacherPane;
    @FXML private VBox      step2StudentPane;

    // Step 1
    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label         usernameError;
    @FXML private Label         passwordError;
    @FXML private Label         confirmError;
    @FXML private Button        roleStudentBtn;
    @FXML private Button        roleTeacherBtn;
    @FXML private Button        roleAdminBtn;

    // Step 2 – Teacher
    @FXML private TextField         teacherFullNameField;
    @FXML private ChoiceBox<String> teacherGenderChoiceBox;
    @FXML private TextField         teacherPhoneField;
    @FXML private TextField         teacherEmailField;
    @FXML private Label             teacherIdLabel;
    @FXML private TextField         subjectField;
    @FXML private TextField         qualificationField;
    @FXML private ChoiceBox<String> employmentStatusChoiceBox;
    @FXML private Label             teacherStatusLabel;

    // Step 2 – Student
    @FXML private TextField         studentFullNameField;
    @FXML private Label             fullNameError;
    @FXML private ChoiceBox<String> genderChoiceBox;
    @FXML private TextField         dobField;
    @FXML private TextField         phoneField;
    @FXML private TextField         studentEmailField;
    @FXML private TextField         addressField;
    @FXML private Label             studentIdLabel;
    @FXML private ChoiceBox<String> classChoiceBox;
    @FXML private TextField         rollNumberField;
    @FXML private TextField         academicSessionField;
    @FXML private TextField         guardianNameField;
    @FXML private ChoiceBox<String> guardianRelationChoiceBox;
    @FXML private TextField         guardianPhoneField;
    @FXML private TextField         guardianEmailField;
    @FXML private Label             studentStatusLabel;


    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        loadClasses();

        genderChoiceBox.setItems(FXCollections.observableArrayList("Male", "Female", "Other"));
        teacherGenderChoiceBox.setItems(FXCollections.observableArrayList("Male", "Female", "Other"));
        employmentStatusChoiceBox.setItems(FXCollections.observableArrayList("Full-time", "Part-time", "Contract"));
        guardianRelationChoiceBox.setItems(FXCollections.observableArrayList("Father", "Mother", "Guardian", "Other"));

        setupTableColumns();
        accountTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        accountTable.setPlaceholder(new Label("No accounts found"));

        filteredAccounts = new FilteredList<>(allAccounts, a -> true);
        searchField.textProperty().addListener((obs, o, n) -> { currentPage = 0; applyFilter(n); });

        refreshView();
    }

    // ── Table setup ───────────────────────────────────────────────────────────

    private void setupTableColumns() {
        userColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        userColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(UserAccount acc, boolean empty) {
                super.updateItem(acc, empty);
                if (empty || acc == null) { setGraphic(null); return; }
                Label avatar = new Label(Utils.initials(acc.getUsername()));
                avatar.setStyle("-fx-background-color: #111111; -fx-text-fill: white; -fx-font-size: 11px; " +
                        "-fx-font-weight: 800; -fx-background-radius: 999; -fx-min-width: 30; -fx-min-height: 30; " +
                        "-fx-pref-width: 30; -fx-pref-height: 30; -fx-alignment: center;");
                Label name = new Label(Utils.formatName(acc.getUsername()));
                name.setStyle("-fx-text-fill: #111111; -fx-font-size: 13px; -fx-font-weight: 700;");
                HBox box = new HBox(8, avatar, name);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });

        usernameColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getUsername()));
        usernameColumn.setCellFactory(col -> greyCell());

        roleColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getRole().getDisplayName()));
        roleColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(v);
                badge.setStyle("-fx-background-color: #f4f4f5; -fx-text-fill: #111111; " +
                        "-fx-padding: 4 10; -fx-background-radius: 999; -fx-font-size: 11px; -fx-font-weight: 700;");
                setGraphic(badge); setText(null);
            }
        });

        phoneColumn.setCellValueFactory(c -> {
            int idx = allAccounts.indexOf(c.getValue());
            return new ReadOnlyStringWrapper(String.format("+977 980000%04d", idx + 1));
        });
        phoneColumn.setCellFactory(col -> greyCell());

        emailColumn.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getUsername().toLowerCase(Locale.ROOT) + "@hamro.edu"));
        emailColumn.setCellFactory(col -> greyCell());

        statusColumn.setCellValueFactory(c -> {
            int idx = allAccounts.indexOf(c.getValue());
            return new ReadOnlyStringWrapper(idx % 4 == 3 ? "Inactive" : "Active");
        });
        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setGraphic(null); setText(null); return; }
                boolean active = "Active".equals(v);
                Label badge = new Label(v);
                badge.setStyle("-fx-background-color:" + (active ? "#dcfce7" : "#f5f5f4") +
                        "; -fx-text-fill:" + (active ? "#16a34a" : "#78716c") +
                        "; -fx-padding: 4 10; -fx-background-radius: 999; -fx-font-size: 11px; -fx-font-weight: 700;");
                setGraphic(badge); setText(null);
            }
        });

        createdColumn.setCellValueFactory(c -> {
            int idx = allAccounts.indexOf(c.getValue());
            return new ReadOnlyStringWrapper(
                    LocalDate.of(2024, 1, 2).plusDays(idx * 3L)
                             .format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        });
        createdColumn.setCellFactory(col -> greyCell());

        actionsColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(UserAccount acc, boolean empty) {
                super.updateItem(acc, empty);
                if (empty || acc == null) { setGraphic(null); return; }
                ImageView icon = new ImageView(new Image(Objects.requireNonNull(
                        getClass().getResourceAsStream("/com/hamroschool/assets/admin/dashboard/three-dot.png"))));
                icon.setFitHeight(14); icon.setFitWidth(14); icon.setPreserveRatio(true);
                Button btn = new Button();
                btn.setGraphic(icon);
                btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6 8; -fx-background-radius: 999;");
                setGraphic(btn);
            }
        });

        selectColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        selectColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(UserAccount acc, boolean empty) {
                super.updateItem(acc, empty);
                if (empty || acc == null) { setGraphic(null); return; }
                Label box = new Label("□");
                box.setStyle("-fx-font-size: 14px; -fx-text-fill: #a8a29e; -fx-cursor: hand;");
                setGraphic(box);
            }
        });
    }

    private TableCell<UserAccount, String> greyCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v);
                setStyle("-fx-text-fill: #44403c; -fx-font-size: 13px;");
            }
        };
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void refreshView() {
        UserAccount cu = SessionContext.getInstance().requireCurrentUser();
        welcomeLabel.setText("Welcome, " + cu.getUsername());
        userInitialsLabel.setText(Utils.initials(cu.getUsername()));
        userNameLabel.setText(Utils.formatName(cu.getUsername()));
        allAccounts.setAll(authService.getAccounts());
        applyFilter(searchField.getText());
        updateStats();
    }

    private void updateStats() {
        totalAccountsLabel.setText(String.valueOf(allAccounts.size()));
        teacherCountLabel.setText(String.valueOf(
                allAccounts.stream().filter(a -> a.getRole() == UserRole.TEACHER).count()));
        studentCountLabel.setText(String.valueOf(
                allAccounts.stream().filter(a -> a.getRole() == UserRole.STUDENT).count()));
    }

    private void applyFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        filteredAccounts.setPredicate(a -> q.isEmpty()
                || a.getUsername().toLowerCase(Locale.ROOT).contains(q)
                || a.getRole().getDisplayName().toLowerCase(Locale.ROOT).contains(q));
        currentPage = 0;
        renderPage();
    }

    private void renderPage() {
        List<UserAccount> all = filteredAccounts.stream().toList();
        int total = all.size();
        int from  = currentPage * PAGE_SIZE;
        int to    = Math.min(from + PAGE_SIZE, total);
        accountTable.setItems(FXCollections.observableArrayList(
                from < total ? all.subList(from, to) : List.of()));
        int pages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        summaryLabel.setText("Showing " + total + " of " + allAccounts.size() + " accounts");
        prevButton.setDisable(currentPage == 0);
        nextButton.setDisable(currentPage >= pages - 1);
    }

    @FXML private void handlePrevPage() { if (currentPage > 0) { currentPage--; renderPage(); } }
    @FXML private void handleNextPage() {
        int pages = (int) Math.ceil((double) filteredAccounts.size() / PAGE_SIZE);
        if (currentPage < pages - 1) { currentPage++; renderPage(); }
    }

    // ── Toolbar stubs ─────────────────────────────────────────────────────────
    @FXML private void handleImportStudents()  { /* stub */ }
    @FXML private void handleExportData()      { /* stub */ }
    @FXML private void handleManageRoles()     { /* stub */ }
    @FXML private void handleFilterRole()      { /* stub */ }
    @FXML private void handleFilterStatus()    { /* stub */ }
    @FXML private void handleSort()            { /* stub */ }
    @FXML private void handleBulkDelete()      { /* stub */ }
    @FXML private void handleBulkDisable()     { /* stub */ }
    @FXML private void handleBulkExport()      { /* stub */ }
    @FXML private void handleBulkChangeRole()  { /* stub */ }

    // ── Modal: open ───────────────────────────────────────────────────────────

    @FXML
    private void handleCreateAccount() {
        resetModal();
        setStep(1);
        modalOverlay.setVisible(true);
        modalOverlay.setManaged(true);
    }

    private void resetModal() {
        // Step 1
        usernameField.clear(); passwordField.clear(); confirmPasswordField.clear();
        hideError(usernameError); hideError(passwordError); hideError(confirmError);
        selectedRole = UserRole.STUDENT;
        refreshRoleButtons();
        // Student
        if (studentFullNameField != null) studentFullNameField.clear();
        if (dobField != null) dobField.clear();
        if (phoneField != null) phoneField.clear();
        if (studentEmailField != null) studentEmailField.clear();
        if (addressField != null) addressField.clear();
        if (rollNumberField != null) rollNumberField.clear();
        if (academicSessionField != null) academicSessionField.clear();
        if (guardianNameField != null) guardianNameField.clear();
        if (guardianPhoneField != null) guardianPhoneField.clear();
        if (guardianEmailField != null) guardianEmailField.clear();
        if (genderChoiceBox != null) genderChoiceBox.getSelectionModel().clearSelection();
        if (guardianRelationChoiceBox != null) guardianRelationChoiceBox.getSelectionModel().clearSelection();
        if (fullNameError != null) hideError(fullNameError);
        if (studentIdLabel != null) studentIdLabel.setText(String.format("STU-%04d", allAccounts.size() + 1));
        if (studentStatusLabel != null) { studentStatusLabel.setVisible(false); studentStatusLabel.setManaged(false); }
        // Teacher
        if (teacherFullNameField != null) teacherFullNameField.clear();
        if (teacherPhoneField != null) teacherPhoneField.clear();
        if (teacherEmailField != null) teacherEmailField.clear();
        if (subjectField != null) subjectField.clear();
        if (qualificationField != null) qualificationField.clear();
        if (teacherGenderChoiceBox != null) teacherGenderChoiceBox.getSelectionModel().clearSelection();
        if (employmentStatusChoiceBox != null) employmentStatusChoiceBox.getSelectionModel().selectFirst();
        if (teacherIdLabel != null) teacherIdLabel.setText(String.format("TCH-%04d", allAccounts.size() + 1));
        if (teacherStatusLabel != null) { teacherStatusLabel.setVisible(false); teacherStatusLabel.setManaged(false); }
    }

    @FXML private void handleCloseModal() {
        modalOverlay.setVisible(false);
        modalOverlay.setManaged(false);
    }

    // ── Modal: role toggle ────────────────────────────────────────────────────

    @FXML private void handleRoleStudent() { selectedRole = UserRole.STUDENT;  refreshRoleButtons(); }
    @FXML private void handleRoleTeacher() { selectedRole = UserRole.TEACHER;  refreshRoleButtons(); }
    @FXML private void handleRoleAdmin()   { selectedRole = UserRole.ADMIN;    refreshRoleButtons(); }

    private void refreshRoleButtons() {
        styleRoleBtn(roleStudentBtn, selectedRole == UserRole.STUDENT);
        styleRoleBtn(roleTeacherBtn, selectedRole == UserRole.TEACHER);
        styleRoleBtn(roleAdminBtn,   selectedRole == UserRole.ADMIN);
    }

    private void styleRoleBtn(Button btn, boolean active) {
        btn.setStyle(active
                ? "-fx-background-color: #111111; -fx-background-radius: 8; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 700; -fx-cursor: hand;"
                : "-fx-background-color: transparent; -fx-background-radius: 8; -fx-text-fill: #44403c; -fx-font-size: 13px; -fx-font-weight: 600; -fx-cursor: hand;");
    }

    // ── Modal: step navigation ────────────────────────────────────────────────

    @FXML
    private void handleStep1Next() {
        boolean ok = true;
        if (usernameField.getText().trim().isEmpty()) { showError(usernameError); ok = false; }
        else hideError(usernameError);
        if (passwordField.getText().isEmpty()) { showError(passwordError); ok = false; }
        else hideError(passwordError);
        if (!passwordField.getText().equals(confirmPasswordField.getText())) { showError(confirmError); ok = false; }
        else hideError(confirmError);
        if (!ok) return;

        if (selectedRole == UserRole.ADMIN) {
            submitAccount(); // Admin has no step 2
        } else {
            setStep(2);
        }
    }

    @FXML private void handleStep2Back() { setStep(1); }

    @FXML
    private void handleSubmitCreateAccount() {
        // Validate student required fields
        if (selectedRole == UserRole.STUDENT) {
            if (studentFullNameField.getText().trim().isEmpty()) {
                showError(fullNameError);
                return;
            }
            hideError(fullNameError);
        }
        submitAccount();
    }

    private void submitAccount() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        Label errLabel = selectedRole == UserRole.TEACHER ? teacherStatusLabel : studentStatusLabel;

        if (!authService.createAccount(username, password, selectedRole)) {
            showStatusError(errLabel, "✗ Username already taken or fields are empty.");
            return;
        }

        if (selectedRole == UserRole.TEACHER) {
            String subject = subjectField != null ? subjectField.getText().trim() : "";
            if (!subject.isEmpty()) teacherService.saveTeacherSubject(username, subject);
        } else if (selectedRole == UserRole.STUDENT) {
            String cls = classChoiceBox != null ? classChoiceBox.getValue() : null;
            if (cls != null && !cls.isEmpty()) classService.enrollStudent(cls, username);
        }

        handleCloseModal();
        refreshView();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setStep(int step) {
        step1Pane.setVisible(step == 1);        step1Pane.setManaged(step == 1);
        step2TeacherPane.setVisible(step == 2 && selectedRole == UserRole.TEACHER);
        step2TeacherPane.setManaged(step == 2 && selectedRole == UserRole.TEACHER);
        step2StudentPane.setVisible(step == 2 && selectedRole == UserRole.STUDENT);
        step2StudentPane.setManaged(step == 2 && selectedRole == UserRole.STUDENT);
    }

    private void showError(Label lbl) { lbl.setVisible(true);  lbl.setManaged(true); }
    private void hideError(Label lbl) { lbl.setVisible(false); lbl.setManaged(false); }
    private void showStatusError(Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #dc2626;");
        lbl.setVisible(true); lbl.setManaged(true);
    }

    private void loadClasses() {
        List<SchoolClass> classes = classService.getAllClasses();
        List<String> names = classes.stream()
                .map(SchoolClass::getClassName)
                .sorted((a, b) -> {
                    try { return Integer.compare(
                            Integer.parseInt(a.replaceAll("\\D+", "")),
                            Integer.parseInt(b.replaceAll("\\D+", ""))); }
                    catch (NumberFormatException e) { return a.compareTo(b); }
                }).toList();
        classChoiceBox.setItems(FXCollections.observableArrayList(names));
        if (!names.isEmpty()) classChoiceBox.getSelectionModel().selectFirst();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML private void handleNavAccounts() {
        SceneSwitcher.showView(welcomeLabel, "/com/hamroschool/account-view.fxml",  "Accounts",  1280, 860);
    }
    @FXML private void handleNavClasses() {
        SceneSwitcher.showView(welcomeLabel, "/com/hamroschool/class-view.fxml",    "Classes",   1280, 860);
    }
    @FXML private void handleNavTeachers() {
        SceneSwitcher.showView(welcomeLabel, "/com/hamroschool/teacher-view.fxml",  "Teachers",  1280, 860);
    }
    @FXML private void handleNavStudents() {
        SceneSwitcher.showView(welcomeLabel, "/com/hamroschool/student-view.fxml",  "Students",  1280, 860);
    }
    @FXML private void handleNavSettings() {
        SceneSwitcher.showView(welcomeLabel, "/com/hamroschool/settings-view.fxml", "Settings",  1280, 860);
    }
    @FXML private void handleLogout() {
        SessionContext.getInstance().clear();
        SceneSwitcher.showView(welcomeLabel, "/com/hamroschool/hello-view.fxml",    "Hamro School", 920, 720);
    }
}
