package com.hamroschool.controller;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.hamroschool.model.auth.UserAccount;
import com.hamroschool.model.auth.UserRole;
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
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class AccountController {

    private static final int PAGE_SIZE = 7;

    private final AuthService    authService    = MongoAuthService.getInstance();
    private final ClassService   classService   = MongoClassService.getInstance();
    private final TeacherService teacherService = TeacherServiceImpl.getInstance();

    private final ObservableList<UserAccount> allAccounts = FXCollections.observableArrayList();
    private List<UserAccount> filteredAccounts = List.of();
    private int currentPage = 0;

    private UserAccount targetAccount = null;

    @FXML private Label userInitialsLabel;
    @FXML private Label userNameLabel;
    @FXML private Label summaryLabel;
    @FXML private TextField searchField;
    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Button logoutButton;

    @FXML private TableView<UserAccount>                accountTable;
    @FXML private TableColumn<UserAccount, UserAccount> userColumn;
    @FXML private TableColumn<UserAccount, String>      usernameColumn;
    @FXML private TableColumn<UserAccount, String>      roleColumn;
    @FXML private TableColumn<UserAccount, UserAccount> actionsColumn;

    @FXML private StackPane profileModalOverlay;
    @FXML private Label     profileAvatarLabel;
    @FXML private Label     profileNameLabel;
    @FXML private Label     profileUsernameLabel;
    @FXML private Label     profileRoleBadge;
    @FXML private Label     profileIdLabel;
    @FXML private Label     profileFullNameVal;
    @FXML private Label     profileGenderVal;
    @FXML private Label     profileDobVal;
    @FXML private Label     profilePhoneVal;
    @FXML private Label     profileEmailVal;
    @FXML private VBox      profileStudentSection;
    @FXML private Label     profileClassVal;
    @FXML private Label     profileRollVal;
    @FXML private Label     profileGuardianVal;
    @FXML private VBox      profileTeacherSection;
    @FXML private Label     profileSubjectVal;
    @FXML private Label     profileQualVal;
    @FXML private Label     profileEmploymentVal;

    @FXML private StackPane    editModalOverlay;
    @FXML private Label        editModalTitle;
    @FXML private Label        editModalSubtitle;
    @FXML private Label        editUsernameDisplay;
    @FXML private Label        editRoleDisplay;
    @FXML private PasswordField editNewPasswordField;
    @FXML private PasswordField editConfirmPasswordField;
    @FXML private Label         editPasswordError;
    @FXML private TextField     editFullNameField;
    @FXML private ChoiceBox<String> editGenderChoiceBox;
    @FXML private TextField     editPhoneField;
    @FXML private TextField     editEmailField;
    @FXML private VBox          editGuardianSection;
    @FXML private VBox          editTeacherSection;
    @FXML private TextField     editSubjectField;
    @FXML private TextField     editQualificationField;
    @FXML private ChoiceBox<String> editEmploymentStatusChoiceBox;
    @FXML private VBox          editStudentSection;
    @FXML private ChoiceBox<String> editClassChoiceBox;
    @FXML private TextField     editRollNumberField;
    @FXML private TextField     editAddressField;
    @FXML private TextField     editAcademicSessionField;
    @FXML private TextField     editGuardianNameField;
    @FXML private ChoiceBox<String> editGuardianRelationChoiceBox;
    @FXML private TextField     editGuardianPhoneField;
    @FXML private TextField     editGuardianEmailField;
    @FXML private Label         editStatusLabel;


    @FXML
    public void initialize() {
        editGenderChoiceBox.setItems(FXCollections.observableArrayList("Male", "Female", "Other"));
        editEmploymentStatusChoiceBox.setItems(FXCollections.observableArrayList("Full-time", "Part-time", "Contract"));
        editGuardianRelationChoiceBox.setItems(FXCollections.observableArrayList("Father", "Mother", "Guardian", "Other"));

        classService.getAllClasses().stream()
            .map(c -> c.getClassName())
            .forEach(name -> editClassChoiceBox.getItems().add(name));

        setupTable();
        refreshCurrentUser();
        loadAccounts();

        searchField.textProperty().addListener((obs, o, n) -> {
            currentPage = 0;
            applyFilterAndPage(n);
        });
    }


    private void setupTable() {
        userColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        userColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(UserAccount acc, boolean empty) {
                super.updateItem(acc, empty);
                if (empty || acc == null) { setGraphic(null); return; }
                Label av = new Label(acc.getFullName().isEmpty()
                    ? Utils.initials(acc.getUsername()) : Utils.initialsFromFull(acc.getFullName()));
                boolean isAdmin = acc.getRole() == UserRole.ADMIN;
                av.setStyle("-fx-background-color: " + (isAdmin ? "#111111" : "#e8e8e6") + "; " +
                    "-fx-text-fill: " + (isAdmin ? "white" : "#444") + "; " +
                    "-fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 999; " +
                    "-fx-min-width: 30; -fx-min-height: 30; -fx-pref-width: 30; -fx-pref-height: 30; -fx-alignment: center;");
                Label name = new Label(acc.getDisplayName());
                name.setStyle("-fx-text-fill: #222; -fx-font-size: 13px; -fx-font-weight: 700;");
                HBox box = new HBox(10, av, name);
                box.setStyle("-fx-alignment: center-left;");
                setGraphic(box);
            }
        });

        usernameColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getUsername()));
        usernameColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v);
                setStyle("-fx-text-fill: #888; -fx-font-size: 13px;");
            }
        });

        roleColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getRole().getDisplayName()));
        roleColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(role);
                boolean isAdmin = "Admin".equals(role);
                badge.setStyle("-fx-background-color: " + (isAdmin ? "#111" : "#f0f0ef") + "; " +
                    "-fx-text-fill: " + (isAdmin ? "white" : "#333") + "; " +
                    "-fx-padding: 4 12; -fx-background-radius: 999; -fx-font-size: 12px; -fx-font-weight: 700;");
                setGraphic(badge); setText(null);
            }
        });

        actionsColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(UserAccount acc, boolean empty) {
                super.updateItem(acc, empty);
                if (empty || acc == null) { setGraphic(null); return; }

                ImageView icon = new ImageView(new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/com/hamroschool/assets/admin/account/three-dot.png"))));
                icon.setFitWidth(16); icon.setFitHeight(16); icon.setPreserveRatio(true);

                Button btn = new Button();
                btn.setGraphic(icon);
                btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; " +
                    "-fx-padding: 6 8; -fx-background-radius: 999;");

                btn.setOnAction(e -> showThreeDotMenu(btn, acc));
                setGraphic(btn);
            }
        });

        accountTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        accountTable.setPlaceholder(new Label("No accounts found"));
        accountTable.setStyle("-fx-background-color: transparent;");
    }

    private void showThreeDotMenu(Button anchor, UserAccount acc) {
        ContextMenu menu = new ContextMenu();

        MenuItem viewItem = new MenuItem("👁  View Details");
        viewItem.setStyle("-fx-font-size: 13px; -fx-font-weight: 600;");
        viewItem.setOnAction(e -> openProfileModal(acc));

        MenuItem editItem = new MenuItem("✏  Edit");
        editItem.setStyle("-fx-font-size: 13px; -fx-font-weight: 600;");
        editItem.setOnAction(e -> openEditModal(acc));

        MenuItem deleteItem = new MenuItem("🗑  Delete");
        deleteItem.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #dc2626;");
        deleteItem.setOnAction(e -> confirmAndDelete(acc));

        String currentUser = SessionContext.getInstance().requireCurrentUser().getUsername();
        if (acc.getUsername().equals(currentUser)) deleteItem.setDisable(true);

        menu.getItems().addAll(viewItem, editItem, new SeparatorMenuItem(), deleteItem);
        menu.show(anchor, Side.BOTTOM, 0, 4);
    }


    private void openProfileModal(UserAccount acc) {
        targetAccount = acc;

        String initials = acc.getFullName().isEmpty()
            ? Utils.initials(acc.getUsername()) : Utils.initialsFromFull(acc.getFullName());
        profileAvatarLabel.setText(initials);
        profileNameLabel.setText(acc.getDisplayName());
        profileUsernameLabel.setText("@" + acc.getUsername());
        profileRoleBadge.setText(acc.getRole().getDisplayName());
        profileIdLabel.setText("ID: " + (acc.getUserId().isEmpty() ? acc.getUsername() : acc.getUserId()));

        profileFullNameVal.setText(orDash(acc.getFullName()));
        profileGenderVal.setText(orDash(acc.getGender()));
        profileDobVal.setText(orDash(acc.getDateOfBirth()));
        profilePhoneVal.setText(orDash(acc.getPhone()));
        profileEmailVal.setText(orDash(acc.getEmail()));

        boolean isStudent = acc.getRole() == UserRole.STUDENT;
        boolean isTeacher = acc.getRole() == UserRole.TEACHER;

        profileStudentSection.setVisible(isStudent); profileStudentSection.setManaged(isStudent);
        profileTeacherSection.setVisible(isTeacher); profileTeacherSection.setManaged(isTeacher);

        if (isStudent) {
            profileClassVal.setText(orDash(acc.getAssignedClass()));
            profileRollVal.setText(orDash(acc.getRollNumber()));
            String guardian = acc.getGuardianName().isEmpty() ? "—"
                : acc.getGuardianName() + " (" + acc.getGuardianRelation() + ")";
            profileGuardianVal.setText(guardian);
        } else if (isTeacher) {
            profileSubjectVal.setText(orDash(acc.getSubject()));
            profileQualVal.setText(orDash(acc.getQualification()));
            profileEmploymentVal.setText(orDash(acc.getEmploymentStatus()));
        }

        profileModalOverlay.setVisible(true);
        profileModalOverlay.setManaged(true);
    }

    @FXML private void handleCloseProfileModal() {
        profileModalOverlay.setVisible(false);
        profileModalOverlay.setManaged(false);
    }

    @FXML private void handleProfileEdit() {
        handleCloseProfileModal();
        if (targetAccount != null) openEditModal(targetAccount);
    }

    @FXML private void handleProfileDelete() {
        handleCloseProfileModal();
        if (targetAccount != null) confirmAndDelete(targetAccount);
    }


    private void openEditModal(UserAccount acc) {
        targetAccount = acc;

        editModalTitle.setText("Edit Account");
        editModalSubtitle.setText("Updating profile for: " + acc.getUsername());
        editUsernameDisplay.setText(acc.getUsername());
        editRoleDisplay.setText(acc.getRole().getDisplayName());

        editNewPasswordField.clear();
        editConfirmPasswordField.clear();
        hideLabel(editPasswordError);
        editStatusLabel.setText("");

        editFullNameField.setText(acc.getFullName());
        editPhoneField.setText(acc.getPhone());
        editEmailField.setText(acc.getEmail());
        editGenderChoiceBox.setValue(acc.getGender().isEmpty() ? null : acc.getGender());

        boolean isTeacher = acc.getRole() == UserRole.TEACHER;
        boolean isStudent = acc.getRole() == UserRole.STUDENT;
        editTeacherSection.setVisible(isTeacher); editTeacherSection.setManaged(isTeacher);
        editStudentSection.setVisible(isStudent); editStudentSection.setManaged(isStudent);
        editGuardianSection.setVisible(isStudent); editGuardianSection.setManaged(isStudent);

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

        editModalOverlay.setVisible(true);
        editModalOverlay.setManaged(true);
    }

    @FXML private void handleCloseEditModal() {
        editModalOverlay.setVisible(false);
        editModalOverlay.setManaged(false);
    }

    @FXML
    private void handleSaveEdit() {
        if (targetAccount == null) return;

        String newPass    = editNewPasswordField.getText();
        String confirmNew = editConfirmPasswordField.getText();
        if (!newPass.isEmpty() && !newPass.equals(confirmNew)) {
            showLabel(editPasswordError);
            return;
        }
        hideLabel(editPasswordError);

        UserAccount updated = new UserAccount(
            targetAccount.getUsername(),
            newPass.isEmpty() ? targetAccount.getPassword() : newPass,
            targetAccount.getRole(),
            editFullNameField.getText().trim(),
            valCB(editGenderChoiceBox),
            targetAccount.getDateOfBirth(),
            editPhoneField.getText().trim(),
            editEmailField.getText().trim(),
            targetAccount.getRole() == UserRole.STUDENT ? editAddressField.getText().trim() : "",
            targetAccount.getUserId(),
            targetAccount.getRole() == UserRole.STUDENT ? valCB(editClassChoiceBox) : "",
            targetAccount.getRole() == UserRole.STUDENT ? editRollNumberField.getText().trim() : "",
            targetAccount.getRole() == UserRole.STUDENT ? editAcademicSessionField.getText().trim() : "",
            targetAccount.getRole() == UserRole.STUDENT ? editGuardianNameField.getText().trim() : "",
            targetAccount.getRole() == UserRole.STUDENT ? valCB(editGuardianRelationChoiceBox) : "",
            targetAccount.getRole() == UserRole.STUDENT ? editGuardianPhoneField.getText().trim() : "",
            targetAccount.getRole() == UserRole.STUDENT ? editGuardianEmailField.getText().trim() : "",
            targetAccount.getRole() == UserRole.TEACHER ? editSubjectField.getText().trim() : "",
            targetAccount.getRole() == UserRole.TEACHER ? editQualificationField.getText().trim() : "",
            targetAccount.getRole() == UserRole.TEACHER ? valCB(editEmploymentStatusChoiceBox) : ""
        );

        Thread t = new Thread(() -> {
            boolean ok = authService.updateAccount(updated);
            if (ok && updated.getRole() == UserRole.TEACHER) {
                teacherService.saveTeacherSubject(updated.getUsername(), updated.getSubject());
            }
            Platform.runLater(() -> {
                if (ok) {
                    handleCloseEditModal();
                    SceneSwitcher.clearCache();
                    loadAccounts();
                } else {
                    editStatusLabel.setText("✗ Save failed. Account not found.");
                    editStatusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #dc2626;");
                }
            });
        }, "AccountSaver");
        t.setDaemon(true);
        t.start();
    }


    private void confirmAndDelete(UserAccount acc) {
        String currentUser = SessionContext.getInstance().requireCurrentUser().getUsername();
        if (acc.getUsername().equals(currentUser)) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Account");
        alert.setHeaderText("Delete " + acc.getDisplayName() + "?");
        alert.setContentText("This will permanently remove the account \"" + acc.getUsername() +
            "\". This action cannot be undone.");
        ButtonType yes = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        ButtonType no  = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(yes, no);

        alert.showAndWait().ifPresent(r -> {
            if (r == yes) {
                Thread t = new Thread(() -> {
                    authService.deleteAccount(acc.getUsername());
                    Platform.runLater(() -> { SceneSwitcher.clearCache(); loadAccounts(); });
                }, "AccountDeleter");
                t.setDaemon(true);
                t.start();
            }
        });
    }


    private void loadAccounts() {
        allAccounts.setAll(authService.getAccounts());
        currentPage = 0;
        applyFilterAndPage(searchField.getText());
    }

    private void applyFilterAndPage(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        filteredAccounts = allAccounts.stream()
            .filter(a -> q.isEmpty()
                || a.getUsername().toLowerCase(Locale.ROOT).contains(q)
                || a.getDisplayName().toLowerCase(Locale.ROOT).contains(q)
                || a.getRole().getDisplayName().toLowerCase(Locale.ROOT).contains(q))
            .toList();

        int totalPages = Math.max(1, (int) Math.ceil((double) filteredAccounts.size() / PAGE_SIZE));
        if (currentPage >= totalPages) currentPage = totalPages - 1;

        int from = currentPage * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, filteredAccounts.size());
        accountTable.setItems(FXCollections.observableArrayList(filteredAccounts.subList(from, to)));

        summaryLabel.setText("Showing " + filteredAccounts.size() + " of " + allAccounts.size() + " accounts");
        prevButton.setDisable(currentPage == 0);
        nextButton.setDisable(currentPage >= totalPages - 1);
    }

    @FXML private void handlePrevPage() {
        if (currentPage > 0) { currentPage--; applyFilterAndPage(searchField.getText()); }
    }

    @FXML private void handleNextPage() {
        int total = Math.max(1, (int) Math.ceil((double) filteredAccounts.size() / PAGE_SIZE));
        if (currentPage < total - 1) { currentPage++; applyFilterAndPage(searchField.getText()); }
    }


    private void refreshCurrentUser() {
        SessionContext.getInstance().getCurrentUser().ifPresent(u -> {
            userInitialsLabel.setText(Utils.initials(u.getUsername()));
            userNameLabel.setText(u.getUsername());
        });
    }

    @FXML private void handleNavDashboard() {
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/admin-view.fxml", "Admin Dashboard", SceneSwitcher.APP_WIDTH, SceneSwitcher.APP_HEIGHT);
    }
    @FXML private void handleNavClasses() {
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/class-view.fxml", "Classes", SceneSwitcher.APP_WIDTH, SceneSwitcher.APP_HEIGHT);
    }
    @FXML private void handleNavTeachers() {
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/teacher-view.fxml", "Teachers", SceneSwitcher.APP_WIDTH, SceneSwitcher.APP_HEIGHT);
    }
    @FXML private void handleNavStudents() {
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/student-view.fxml", "Students", SceneSwitcher.APP_WIDTH, SceneSwitcher.APP_HEIGHT);
    }
    @FXML private void handleNavSettings() {
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/settings-view.fxml", "Settings", SceneSwitcher.APP_WIDTH, SceneSwitcher.APP_HEIGHT);
    }
    @FXML private void handleLogout() {
        SessionContext.getInstance().clear();
        SceneSwitcher.clearCache();
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/hello-view.fxml", "Hamro School", SceneSwitcher.LOGIN_WIDTH, SceneSwitcher.LOGIN_HEIGHT);
    }


    private String orDash(String v)             { return (v == null || v.isBlank()) ? "—" : v; }
    private String valCB(ChoiceBox<String> cb)  { String v = cb.getValue(); return v == null ? "" : v; }
    private void showLabel(Label l)             { l.setVisible(true);  l.setManaged(true);  }
    private void hideLabel(Label l)             { l.setVisible(false); l.setManaged(false); }
}
