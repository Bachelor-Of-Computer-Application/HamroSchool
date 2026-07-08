package com.hamroschool.controller;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.hamroschool.model.auth.UserAccount;
import com.hamroschool.model.entity.SchoolClass;
import com.hamroschool.service.ClassService;
import com.hamroschool.service.impl.MongoClassService;
import com.hamroschool.util.SceneSwitcher;
import com.hamroschool.util.SessionContext;
import com.hamroschool.util.Utils;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;

public class ClassController {

    private final ClassService classService = MongoClassService.getInstance();

    private volatile List<SchoolClass> cachedClasses = List.of();
    private volatile boolean dataLoaded = false;

    @FXML private Label userInitialsLabel;
    @FXML private Label userNameLabel;
    @FXML private Button logoutButton;

    @FXML private TextField searchField;
    @FXML private Button addClassBtn;
    @FXML private Label classSummaryLabel;

    @FXML private TableView<SchoolClass> classTable;
    @FXML private TableColumn<SchoolClass, String> colClassName;
    @FXML private TableColumn<SchoolClass, Integer> colTeachers;
    @FXML private TableColumn<SchoolClass, Integer> colStudents;
    @FXML private TableColumn<SchoolClass, String> colCreatedDate;
    @FXML private TableColumn<SchoolClass, SchoolClass> colActions;

    @FXML private VBox detailsPane;
    @FXML private Label detailsClassName;
    @FXML private Label detailsTeacherCount;
    @FXML private Label detailsStudentCount;
    
    @FXML private Button editClassNameBtn;
    @FXML private Button deleteClassBtn;
    @FXML private Button closeDetailsBtn;
    
    @FXML private TableView<String> teachersTable;
    @FXML private TableColumn<String, String> colTeacherName;
    @FXML private TableColumn<String, String> colTeacherActions;
    
    @FXML private TableView<String> studentsTable;
    @FXML private TableColumn<String, String> colStudentName;
    @FXML private TableColumn<String, String> colStudentActions;
    
    @FXML private Button addTeacherBtn;
    @FXML private Button addStudentBtn;

    private SchoolClass selectedClass;


    @FXML
    public void initialize() {
        UserAccount currentUser = SessionContext.getInstance().requireCurrentUser();
        userInitialsLabel.setText(Utils.initials(currentUser.getUsername()));
        userNameLabel.setText(currentUser.getUsername());

        setupClassTable();
        setupDetailsTable();
        
        detailsPane.setVisible(false);
        detailsPane.setManaged(false);

        searchField.textProperty().addListener((obs, o, n) -> filterClasses(n));
        addClassBtn.setOnAction(e -> handleAddClass());
        
        Thread loader = new Thread(() -> {
            try {
                List<SchoolClass> classes = classService.getAllClasses();
                
                cachedClasses = classes;
                dataLoaded = true;
                
                javafx.application.Platform.runLater(this::loadClasses);
            } catch (Exception ex) {
                System.err.println("[ClassController] Data load error: " + ex.getMessage());
            }
        }, "ClassDataLoader");
        loader.setDaemon(true);
        loader.start();
    }


    private void setupClassTable() {
        colClassName.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getClassName()));
        colClassName.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String className, boolean empty) {
                super.updateItem(className, empty);
                if (empty || className == null) {
                    setGraphic(null);
                    return;
                }
                Label nameLabel = new Label(className);
                nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #111111;");
                setGraphic(nameLabel);
            }
        });

        colTeachers.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getTeacherCount()));
        colTeachers.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer count, boolean empty) {
                super.updateItem(count, empty);
                if (empty || count == null) {
                    setText(null);
                    return;
                }
                setText(count + " teacher" + (count == 1 ? "" : "s"));
                setStyle("-fx-font-size: 13px; -fx-text-fill: #44403c;");
            }
        });

        colStudents.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getStudentCount()));
        colStudents.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer count, boolean empty) {
                super.updateItem(count, empty);
                if (empty || count == null) {
                    setText(null);
                    return;
                }
                setText(count + " student" + (count == 1 ? "" : "s"));
                setStyle("-fx-font-size: 13px; -fx-text-fill: #44403c;");
            }
        });

        colCreatedDate.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getCreatedDate() != null 
                    ? c.getValue().getCreatedDate().substring(0, Math.min(10, c.getValue().getCreatedDate().length()))
                    : "—"
        ));
        colCreatedDate.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                    return;
                }
                setText(date);
                setStyle("-fx-font-size: 13px; -fx-text-fill: #78716c;");
            }
        });

        colActions.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("View Details");
            
            {
                viewBtn.setStyle("-fx-background-color: #111111; -fx-text-fill: white; " +
                        "-fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 6 14; " +
                        "-fx-background-radius: 6; -fx-cursor: hand;");
                viewBtn.setOnAction(e -> {
                    SchoolClass schoolClass = getTableRow().getItem();
                    if (schoolClass != null) {
                        showClassDetails(schoolClass);
                    }
                });
            }
            
            @Override
            protected void updateItem(SchoolClass schoolClass, boolean empty) {
                super.updateItem(schoolClass, empty);
                if (empty || schoolClass == null) {
                    setGraphic(null);
                    return;
                }
                setGraphic(viewBtn);
            }
        });

        classTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        classTable.setPlaceholder(new Label("No classes found. Click 'Add Class' to create one."));
    }

    private void setupDetailsTable() {
        colTeacherName.setCellValueFactory(c -> new ReadOnlyStringWrapper(Utils.formatName(c.getValue())));
        colTeacherActions.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue()));
        colTeacherActions.setCellFactory(col -> new TableCell<>() {
            private final Button removeBtn = new Button("Remove");
            
            {
                removeBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; " +
                        "-fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 4 12; " +
                        "-fx-background-radius: 4; -fx-cursor: hand;");
                removeBtn.setOnAction(e -> {
                    String teacher = getTableRow().getItem();
                    if (teacher != null && selectedClass != null) {
                        handleRemoveTeacher(teacher);
                    }
                });
            }
            
            @Override
            protected void updateItem(String teacher, boolean empty) {
                super.updateItem(teacher, empty);
                setGraphic(empty || teacher == null ? null : removeBtn);
            }
        });

        colStudentName.setCellValueFactory(c -> new ReadOnlyStringWrapper(Utils.formatName(c.getValue())));
        colStudentActions.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue()));
        colStudentActions.setCellFactory(col -> new TableCell<>() {
            private final Button removeBtn = new Button("Remove");
            
            {
                removeBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; " +
                        "-fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 4 12; " +
                        "-fx-background-radius: 4; -fx-cursor: hand;");
                removeBtn.setOnAction(e -> {
                    String student = getTableRow().getItem();
                    if (student != null && selectedClass != null) {
                        handleRemoveStudent(student);
                    }
                });
            }
            
            @Override
            protected void updateItem(String student, boolean empty) {
                super.updateItem(student, empty);
                setGraphic(empty || student == null ? null : removeBtn);
            }
        });

        addTeacherBtn.setOnAction(e -> handleAddTeacher());
        addStudentBtn.setOnAction(e -> handleAddStudent());
        editClassNameBtn.setOnAction(e -> handleEditClassName());
        deleteClassBtn.setOnAction(e -> handleDeleteClass());
        closeDetailsBtn.setOnAction(e -> hideClassDetails());
    }


    private void loadClasses() {
        if (!dataLoaded) return;
        filterClasses(searchField.getText());
    }

    private void filterClasses(String query) {
        if (!dataLoaded) return;
        
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<SchoolClass> filtered = q.isBlank() ? cachedClasses
                : cachedClasses.stream()
                    .filter(c -> c.getClassName().toLowerCase(Locale.ROOT).contains(q))
                    .toList();
        
        classTable.setItems(FXCollections.observableArrayList(filtered));
        classSummaryLabel.setText("Showing " + filtered.size() + " of " + cachedClasses.size() + " classes");
    }


    @FXML
    private void handleAddClass() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add New Class");
        dialog.setHeaderText("Create a new class");
        dialog.setContentText("Class Name:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(className -> {
            if (className.isBlank()) {
                showError("Class name cannot be empty.");
                return;
            }
            try {
                String currentUser = SessionContext.getInstance().requireCurrentUser().getUsername();
                classService.createClass(className.trim(), currentUser);
                refreshData();
                showSuccess("Class created successfully!");
            } catch (Exception ex) {
                showError("Failed to create class: " + ex.getMessage());
            }
        });
    }

    private void showClassDetails(SchoolClass schoolClass) {
        selectedClass = schoolClass;
        detailsClassName.setText(schoolClass.getClassName());
        detailsTeacherCount.setText(schoolClass.getTeacherCount() + " teachers");
        detailsStudentCount.setText(schoolClass.getStudentCount() + " students");
        
        teachersTable.setItems(FXCollections.observableArrayList(schoolClass.getAssignedTeachers()));
        studentsTable.setItems(FXCollections.observableArrayList(schoolClass.getEnrolledStudents()));
        
        detailsPane.setVisible(true);
        detailsPane.setManaged(true);
    }

    private void hideClassDetails() {
        selectedClass = null;
        detailsPane.setVisible(false);
        detailsPane.setManaged(false);
    }

    @FXML
    private void handleEditClassName() {
        if (selectedClass == null) return;
        
        TextInputDialog dialog = new TextInputDialog(selectedClass.getClassName());
        dialog.setTitle("Edit Class Name");
        dialog.setHeaderText("Edit class name");
        dialog.setContentText("New Class Name:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            if (newName.isBlank()) {
                showError("Class name cannot be empty.");
                return;
            }
            try {
                classService.updateClass(selectedClass.getClassName(), newName.trim());
                refreshData();
                showSuccess("Class name updated successfully!");
            } catch (Exception ex) {
                showError("Failed to update class: " + ex.getMessage());
            }
        });
    }

    @FXML
    private void handleDeleteClass() {
        if (selectedClass == null) return;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Class");
        alert.setHeaderText("Delete " + selectedClass.getClassName() + "?");
        alert.setContentText("This will remove all teacher assignments and student enrollments. This action cannot be undone.");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                classService.deleteClass(selectedClass.getClassName());
                hideClassDetails();
                refreshData();
                showSuccess("Class deleted successfully!");
            } catch (Exception ex) {
                showError("Failed to delete class: " + ex.getMessage());
            }
        }
    }

    private void handleAddTeacher() {
        if (selectedClass == null) return;
        
        List<String> availableTeachers = classService.getUnassignedTeachers();
        if (availableTeachers.isEmpty()) {
            showError("No available teachers to assign.");
            return;
        }
        
        ChoiceDialog<String> dialog = new ChoiceDialog<>(availableTeachers.get(0), availableTeachers);
        dialog.setTitle("Assign Teacher");
        dialog.setHeaderText("Assign a teacher to " + selectedClass.getClassName());
        dialog.setContentText("Select Teacher:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(teacher -> {
            try {
                classService.assignTeacher(selectedClass.getClassName(), teacher);
                refreshClassDetails();
                showSuccess("Teacher assigned successfully!");
            } catch (Exception ex) {
                showError("Failed to assign teacher: " + ex.getMessage());
            }
        });
    }

    private void handleRemoveTeacher(String teacher) {
        if (selectedClass == null) return;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Remove Teacher");
        alert.setHeaderText("Remove " + Utils.formatName(teacher) + " from " + selectedClass.getClassName() + "?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                classService.removeTeacher(selectedClass.getClassName(), teacher);
                refreshClassDetails();
                showSuccess("Teacher removed successfully!");
            } catch (Exception ex) {
                showError("Failed to remove teacher: " + ex.getMessage());
            }
        }
    }

    private void handleAddStudent() {
        if (selectedClass == null) return;
        
        List<String> availableStudents = classService.getUnassignedStudents();
        if (availableStudents.isEmpty()) {
            showError("No available students to enroll.");
            return;
        }
        
        ChoiceDialog<String> dialog = new ChoiceDialog<>(availableStudents.get(0), availableStudents);
        dialog.setTitle("Enroll Student");
        dialog.setHeaderText("Enroll a student in " + selectedClass.getClassName());
        dialog.setContentText("Select Student:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(student -> {
            try {
                classService.enrollStudent(selectedClass.getClassName(), student);
                refreshClassDetails();
                showSuccess("Student enrolled successfully!");
            } catch (Exception ex) {
                showError("Failed to enroll student: " + ex.getMessage());
            }
        });
    }

    private void handleRemoveStudent(String student) {
        if (selectedClass == null) return;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Remove Student");
        alert.setHeaderText("Remove " + Utils.formatName(student) + " from " + selectedClass.getClassName() + "?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                classService.removeStudent(selectedClass.getClassName(), student);
                refreshClassDetails();
                showSuccess("Student removed successfully!");
            } catch (Exception ex) {
                showError("Failed to remove student: " + ex.getMessage());
            }
        }
    }

    @FXML
    private void handleLogout() {
        SessionContext.getInstance().clear();
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/hello-view.fxml", "Hamro School", 920, 720);
    }


    private void refreshData() {
        Thread loader = new Thread(() -> {
            cachedClasses = classService.getAllClasses();
            javafx.application.Platform.runLater(this::loadClasses);
        });
        loader.setDaemon(true);
        loader.start();
    }

    private void refreshClassDetails() {
        if (selectedClass == null) return;
        
        Optional<SchoolClass> updated = classService.getClassByName(selectedClass.getClassName());
        updated.ifPresent(this::showClassDetails);
        refreshData();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    @FXML
    private void handleNavDashboard() {
        SceneSwitcher.showView(userNameLabel, "/com/hamroschool/admin-view.fxml", "Admin Dashboard", 1280, 860);
    }

    @FXML
    private void handleNavAccounts() {
        SceneSwitcher.showView(userNameLabel, "/com/hamroschool/account-view.fxml", "Accounts", 1280, 860);
    }

    @FXML
    private void handleNavTeachers() {
        SceneSwitcher.showView(userNameLabel, "/com/hamroschool/teacher-view.fxml", "Teachers", 1280, 860);
    }

    @FXML
    private void handleNavStudents() {
        SceneSwitcher.showView(userNameLabel, "/com/hamroschool/student-view.fxml", "Students", 1280, 860);
    }

    @FXML
    private void handleNavSettings() {
        SceneSwitcher.showView(userNameLabel, "/com/hamroschool/settings-view.fxml", "Settings", 1280, 860);
    }
}
