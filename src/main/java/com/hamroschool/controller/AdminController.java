package com.hamroschool.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

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

import javafx.application.Platform;
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

    private UserAccount editingAccount = null;

    @FXML private Label welcomeLabel;
    @FXML private Label userInitialsLabel;
    @FXML private Label userNameLabel;

    @FXML private Label totalAccountsLabel;
    @FXML private Label teacherCountLabel;
    @FXML private Label studentCountLabel;

    @FXML private TextField  searchField;
    @FXML private Button     filterRoleBtn;
    @FXML private Button     filterStatusBtn;
    @FXML private Button     sortBtn;
    @FXML private HBox       bulkActionBar;
    @FXML private Label      selectedCountLabel;

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

    @FXML private StackPane modalOverlay;
    @FXML private VBox      step1Pane;
    @FXML private VBox      step2TeacherPane;
    @FXML private VBox      step2StudentPane;

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label         usernameError;
    @FXML private Label         passwordError;
    @FXML private Label         confirmError;
    @FXML private Button        roleStudentBtn;
    @FXML private Button        roleTeacherBtn;
    @FXML private Button        roleAdminBtn;

    @FXML private TextField         teacherFullNameField;
    @FXML private ChoiceBox<String> teacherGenderChoiceBox;
    @FXML private TextField         teacherPhoneField;
    @FXML private TextField         teacherEmailField;
    @FXML private Label             teacherIdLabel;
    @FXML private TextField         subjectField;
    @FXML private TextField         qualificationField;
    @FXML private ChoiceBox<String> employmentStatusChoiceBox;
    @FXML private Label             teacherStatusLabel;

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

    @FXML private StackPane editModalOverlay;
    @FXML private Label     editModalTitle;
    @FXML private Label     editModalSubtitle;

    @FXML private Label         editUsernameDisplay;
    @FXML private Label         editRoleDisplay;
    @FXML private PasswordField editNewPasswordField;
    @FXML private PasswordField editConfirmPasswordField;
    @FXML private Label         editPasswordError;

    @FXML private TextField         editFullNameField;
    @FXML private ChoiceBox<String> editGenderChoiceBox;
    @FXML private TextField         editPhoneField;
    @FXML private TextField         editEmailField;

    @FXML private VBox              editTeacherSection;
    @FXML private TextField         editSubjectField;
    @FXML private TextField         editQualificationField;
    @FXML private ChoiceBox<String> editEmploymentStatusChoiceBox;

    @FXML private VBox              editStudentSection;
    @FXML private TextField         editAddressField;
    @FXML private ChoiceBox<String> editClassChoiceBox;
    @FXML private TextField         editRollNumberField;
    @FXML private TextField         editAcademicSessionField;
    @FXML private TextField         editGuardianNameField;
    @FXML private ChoiceBox<String> editGuardianRelationChoiceBox;
    @FXML private TextField         editGuardianPhoneField;
    @FXML private TextField         editGuardianEmailField;

    @FXML private Label  editStatusLabel;
    @FXML private Button editDeleteBtn;


    @FXML
    public void initialize() {
        loadClasses();

        genderChoiceBox.setItems(FXCollections.observableArrayList("Male", "Female", "Other"));
        teacherGenderChoiceBox.setItems(FXCollections.observableArrayList("Male", "Female", "Other"));
        employmentStatusChoiceBox.setItems(FXCollections.observableArrayList("Full-time", "Part-time", "Contract"));
        guardianRelationChoiceBox.setItems(FXCollections.observableArrayList("Father", "Mother", "Guardian", "Other"));

        editGenderChoiceBox.setItems(FXCollections.observableArrayList("Male", "Female", "Other"));
        editEmploymentStatusChoiceBox.setItems(FXCollections.observableArrayList("Full-time", "Part-time", "Contract"));
        editGuardianRelationChoiceBox.setItems(FXCollections.observableArrayList("Father", "Mother", "Guardian", "Other"));

        classChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null && rollNumberField != null) updateRollNumberForClass(n);
        });

        setupTableColumns();
        accountTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        accountTable.setPlaceholder(new Label("No accounts found"));

        filteredAccounts = new FilteredList<>(allAccounts, a -> true);
        searchField.textProperty().addListener((obs, o, n) -> { currentPage = 0; applyFilter(n); });

        refreshView();
    }


    private void setupTableColumns() {
        userColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        userColumn.setCellFactory(col -> new TableCell<>() {
            private final Label avatar = new Label();
            private final Label name   = new Label();
            private final HBox  box    = new HBox(8, avatar, name);
            {
                avatar.setStyle("-fx-background-color: #111111; -fx-text-fill: white; -fx-font-size: 11px; " +
                        "-fx-font-weight: 800; -fx-background-radius: 999; -fx-min-width: 30; -fx-min-height: 30; " +
                        "-fx-pref-width: 30; -fx-pref-height: 30; -fx-alignment: center;");
                name.setStyle("-fx-text-fill: #111111; -fx-font-size: 13px; -fx-font-weight: 700;");
                box.setAlignment(Pos.CENTER_LEFT);
            }
            @Override protected void updateItem(UserAccount acc, boolean empty) {
                super.updateItem(acc, empty);
                if (empty || acc == null) { setGraphic(null); return; }
                avatar.setText(acc.getFullName().isEmpty()
                        ? Utils.initials(acc.getUsername()) : Utils.initialsFromFull(acc.getFullName()));
                name.setText(acc.getDisplayName());
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
            String p = c.getValue().getPhone();
            return new ReadOnlyStringWrapper(p.isEmpty() ? "—" : p);
        });
        phoneColumn.setCellFactory(col -> greyCell());

        emailColumn.setCellValueFactory(c -> {
            String e = c.getValue().getEmail();
            return new ReadOnlyStringWrapper(e.isEmpty() ? "—" : e);
        });
        emailColumn.setCellFactory(col -> greyCell());

        statusColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper("Active"));
        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(v);
                badge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; " +
                        "-fx-padding: 4 10; -fx-background-radius: 999; -fx-font-size: 11px; -fx-font-weight: 700;");
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
            private final Button editBtn   = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox   box       = new HBox(6, editBtn, deleteBtn);
            {
                editBtn.setStyle("-fx-background-color: #111111; -fx-background-radius: 6; " +
                        "-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: 700; " +
                        "-fx-padding: 4 10; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #fca5a5; " +
                        "-fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: #dc2626; " +
                        "-fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 4 10; -fx-cursor: hand;");
                box.setAlignment(Pos.CENTER_LEFT);
            }
            @Override protected void updateItem(UserAccount acc, boolean empty) {
                super.updateItem(acc, empty);
                if (empty || acc == null) { setGraphic(null); return; }
                editBtn.setOnAction(e -> openEditModal(acc));
                deleteBtn.setOnAction(e -> handleDeleteAccount(acc));
                setGraphic(box);
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
                || a.getDisplayName().toLowerCase(Locale.ROOT).contains(q)
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


    @FXML
    private void handleCreateAccount() {
        editingAccount = null;
        resetCreateModal();
        setStep(1);
        modalOverlay.setVisible(true);
        modalOverlay.setManaged(true);
    }

    private void resetCreateModal() {
        usernameField.clear(); passwordField.clear(); confirmPasswordField.clear();
        usernameField.setDisable(false);
        hideError(usernameError); hideError(passwordError); hideError(confirmError);
        selectedRole = UserRole.STUDENT;
        refreshRoleButtons();
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
        if (selectedRole == UserRole.ADMIN) { submitCreateAccount(); }
        else { setStep(2); }
    }

    @FXML private void handleStep2Back() { setStep(1); }

    @FXML
    private void handleSubmitCreateAccount() {
        if (selectedRole == UserRole.STUDENT) {
            if (studentFullNameField.getText().trim().isEmpty()) { showError(fullNameError); return; }
            hideError(fullNameError);
        }
        submitCreateAccount();
    }

    private void submitCreateAccount() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        Label errLabel  = selectedRole == UserRole.TEACHER ? teacherStatusLabel : studentStatusLabel;

        UserAccount account;
        if (selectedRole == UserRole.STUDENT) {
            account = new UserAccount(username, password, UserRole.STUDENT,
                    val(studentFullNameField), val(genderChoiceBox),
                    val(dobField), val(phoneField), val(studentEmailField),
                    val(addressField), studentIdLabel != null ? studentIdLabel.getText() : "",
                    val(classChoiceBox), val(rollNumberField), val(academicSessionField),
                    val(guardianNameField), val(guardianRelationChoiceBox),
                    val(guardianPhoneField), val(guardianEmailField), "", "", "");
        } else if (selectedRole == UserRole.TEACHER) {
            account = new UserAccount(username, password, UserRole.TEACHER,
                    val(teacherFullNameField), val(teacherGenderChoiceBox),
                    "", val(teacherPhoneField), val(teacherEmailField),
                    "", teacherIdLabel != null ? teacherIdLabel.getText() : "",
                    "", "", "", "", "", "", "",
                    val(subjectField), val(qualificationField), val(employmentStatusChoiceBox));
        } else {
            account = new UserAccount(username, password, UserRole.ADMIN);
        }

        if (!authService.createFullAccount(account)) {
            showStatusError(errLabel, "✗ Username already taken or fields are empty.");
            return;
        }
        if (selectedRole == UserRole.TEACHER && !val(subjectField).isEmpty())
            teacherService.saveTeacherSubject(username, val(subjectField));
        if (selectedRole == UserRole.STUDENT && !val(classChoiceBox).isEmpty())
            classService.enrollStudent(val(classChoiceBox), username);

        handleCloseModal();
        SceneSwitcher.clearCache();
        refreshView();
    }


    private void openEditModal(UserAccount acc) {
        editingAccount = acc;

        editModalTitle.setText("Edit Account");
        editModalSubtitle.setText("Editing: " + acc.getUsername());
        editUsernameDisplay.setText(acc.getUsername());
        editRoleDisplay.setText(acc.getRole().getDisplayName());

        editFullNameField.setText(acc.getFullName());
        editPhoneField.setText(acc.getPhone());
        editEmailField.setText(acc.getEmail());
        editGenderChoiceBox.setValue(acc.getGender().isEmpty() ? null : acc.getGender());

        editNewPasswordField.clear();
        editConfirmPasswordField.clear();
        hideError(editPasswordError);
        editStatusLabel.setText("");

        boolean isTeacher = acc.getRole() == UserRole.TEACHER;
        boolean isStudent = acc.getRole() == UserRole.STUDENT;
        editTeacherSection.setVisible(isTeacher); editTeacherSection.setManaged(isTeacher);
        editStudentSection.setVisible(isStudent); editStudentSection.setManaged(isStudent);

        if (isTeacher) {
            editSubjectField.setText(acc.getSubject());
            editQualificationField.setText(acc.getQualification());
            editEmploymentStatusChoiceBox.setValue(
                    acc.getEmploymentStatus().isEmpty() ? "Full-time" : acc.getEmploymentStatus());
        } else if (isStudent) {
            editAddressField.setText(acc.getAddress());
            editClassChoiceBox.setValue(acc.getAssignedClass().isEmpty() ? null : acc.getAssignedClass());
            editRollNumberField.setText(acc.getRollNumber());
            editAcademicSessionField.setText(acc.getAcademicSession());
            editGuardianNameField.setText(acc.getGuardianName());
            editGuardianRelationChoiceBox.setValue(
                    acc.getGuardianRelation().isEmpty() ? null : acc.getGuardianRelation());
            editGuardianPhoneField.setText(acc.getGuardianPhone());
            editGuardianEmailField.setText(acc.getGuardianEmail());
        }

        String currentUser = SessionContext.getInstance().requireCurrentUser().getUsername();
        editDeleteBtn.setDisable(acc.getUsername().equals(currentUser));

        editModalOverlay.setVisible(true);
        editModalOverlay.setManaged(true);
    }

    @FXML private void handleCloseEditModal() {
        editModalOverlay.setVisible(false);
        editModalOverlay.setManaged(false);
        editingAccount = null;
    }

    @FXML
    private void handleSaveEdit() {
        if (editingAccount == null) return;

        String newPass    = editNewPasswordField.getText();
        String confirmNew = editConfirmPasswordField.getText();
        if (!newPass.isEmpty() && !newPass.equals(confirmNew)) {
            showError(editPasswordError);
            return;
        }
        hideError(editPasswordError);

        UserAccount updated = new UserAccount(
                editingAccount.getUsername(),
                newPass.isEmpty() ? editingAccount.getPassword() : newPass,
                editingAccount.getRole(),
                editFullNameField.getText().trim(),
                valCB(editGenderChoiceBox),
                editingAccount.getDateOfBirth(),  // DOB not editable here
                editPhoneField.getText().trim(),
                editEmailField.getText().trim(),
                editingAccount.getRole() == UserRole.STUDENT ? editAddressField.getText().trim() : "",
                editingAccount.getUserId(),
                editingAccount.getRole() == UserRole.STUDENT ? valCB(editClassChoiceBox) : "",
                editingAccount.getRole() == UserRole.STUDENT ? editRollNumberField.getText().trim() : "",
                editingAccount.getRole() == UserRole.STUDENT ? editAcademicSessionField.getText().trim() : "",
                editingAccount.getRole() == UserRole.STUDENT ? editGuardianNameField.getText().trim() : "",
                editingAccount.getRole() == UserRole.STUDENT ? valCB(editGuardianRelationChoiceBox) : "",
                editingAccount.getRole() == UserRole.STUDENT ? editGuardianPhoneField.getText().trim() : "",
                editingAccount.getRole() == UserRole.STUDENT ? editGuardianEmailField.getText().trim() : "",
                editingAccount.getRole() == UserRole.TEACHER ? editSubjectField.getText().trim() : "",
                editingAccount.getRole() == UserRole.TEACHER ? editQualificationField.getText().trim() : "",
                editingAccount.getRole() == UserRole.TEACHER ? valCB(editEmploymentStatusChoiceBox) : ""
        );

        Thread saver = new Thread(() -> {
            boolean ok = authService.updateAccount(updated);
            if (ok && updated.getRole() == UserRole.TEACHER) {
                teacherService.saveTeacherSubject(updated.getUsername(), updated.getSubject());
            }
            Platform.runLater(() -> {
                if (ok) {
                    handleCloseEditModal();
                    SceneSwitcher.clearCache();
                    refreshView();
                } else {
                    editStatusLabel.setText("✗ Save failed. Account not found.");
                    editStatusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #dc2626;");
                }
            });
        }, "AccountSaver");
        saver.setDaemon(true);
        saver.start();
    }

    private void handleDeleteAccount(UserAccount acc) {
        String currentUser = SessionContext.getInstance().requireCurrentUser().getUsername();
        if (acc.getUsername().equals(currentUser)) return; // can't delete yourself

        Thread deleter = new Thread(() -> {
            authService.deleteAccount(acc.getUsername());
            Platform.runLater(() -> {
                SceneSwitcher.clearCache();
                refreshView();
            });
        }, "AccountDeleter");
        deleter.setDaemon(true);
        deleter.start();
    }

    @FXML
    private void handleEditDelete() {
        if (editingAccount == null) return;
        handleDeleteAccount(editingAccount);
        handleCloseEditModal();
    }


    private String val(TextField f)         { return f == null ? "" : f.getText().trim(); }
    private String val(ChoiceBox<String> cb) {
        if (cb == null) return "";
        String v = cb.getValue(); return v == null ? "" : v;
    }
    private String valCB(ChoiceBox<String> cb) { return val(cb); }

    private void setStep(int step) {
        step1Pane.setVisible(step == 1);        step1Pane.setManaged(step == 1);
        step2TeacherPane.setVisible(step == 2 && selectedRole == UserRole.TEACHER);
        step2TeacherPane.setManaged(step == 2  && selectedRole == UserRole.TEACHER);
        step2StudentPane.setVisible(step == 2 && selectedRole == UserRole.STUDENT);
        step2StudentPane.setManaged(step == 2  && selectedRole == UserRole.STUDENT);
    }

    private void showError(Label lbl)               { lbl.setVisible(true);  lbl.setManaged(true); }
    private void hideError(Label lbl)               { lbl.setVisible(false); lbl.setManaged(false); }
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
        editClassChoiceBox.setItems(FXCollections.observableArrayList(names));
        if (!names.isEmpty()) classChoiceBox.getSelectionModel().selectFirst();
    }

    private void updateRollNumberForClass(String className) {
        var classOpt = classService.getClassByName(className);
        if (classOpt.isEmpty()) { rollNumberField.setText("1"); return; }
        SchoolClass schoolClass = classOpt.get();
        List<String> enrolled = schoolClass.getEnrolledStudents();
        if (enrolled == null || enrolled.isEmpty()) { rollNumberField.setText("1"); return; }
        int maxRoll = 0;
        for (String su : enrolled) {
            try {
                var acc = allAccounts.stream()
                        .filter(a -> a.getUsername().equals(su) && a.getRole() == UserRole.STUDENT)
                        .findFirst();
                if (acc.isPresent() && !acc.get().getRollNumber().isEmpty())
                    maxRoll = Math.max(maxRoll, Integer.parseInt(acc.get().getRollNumber()));
            } catch (NumberFormatException ignored) {}
        }
        rollNumberField.setText(String.valueOf(maxRoll + 1));
    }


    @FXML private void handleNavAccounts() {
        SceneSwitcher.showView(welcomeLabel, "/com/hamroschool/account-view.fxml",  "Accounts",   SceneSwitcher.APP_WIDTH, SceneSwitcher.APP_HEIGHT);
    }
    @FXML private void handleNavClasses() {
        SceneSwitcher.showView(welcomeLabel, "/com/hamroschool/class-view.fxml",    "Classes",    SceneSwitcher.APP_WIDTH, SceneSwitcher.APP_HEIGHT);
    }
    @FXML private void handleNavTeachers() {
        SceneSwitcher.showView(welcomeLabel, "/com/hamroschool/teacher-view.fxml",  "Teachers",   SceneSwitcher.APP_WIDTH, SceneSwitcher.APP_HEIGHT);
    }
    @FXML private void handleNavStudents() {
        SceneSwitcher.showView(welcomeLabel, "/com/hamroschool/student-view.fxml",  "Students",   SceneSwitcher.APP_WIDTH, SceneSwitcher.APP_HEIGHT);
    }
    @FXML private void handleNavSettings() {
        SceneSwitcher.showView(welcomeLabel, "/com/hamroschool/settings-view.fxml", "Settings",   SceneSwitcher.APP_WIDTH, SceneSwitcher.APP_HEIGHT);
    }
    @FXML private void handleLogout() {
        SessionContext.getInstance().clear();
        SceneSwitcher.clearCache();
        SceneSwitcher.showView(welcomeLabel, "/com/hamroschool/hello-view.fxml", "Hamro School", SceneSwitcher.LOGIN_WIDTH, SceneSwitcher.LOGIN_HEIGHT);
    }
}
