package com.hamroschool.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.hamroschool.model.entity.Mark;
import com.hamroschool.service.AttendanceService;
import com.hamroschool.service.MarkService;
import com.hamroschool.service.TeacherService;
import com.hamroschool.service.impl.AttendanceServiceImpl;
import com.hamroschool.service.impl.MarkServiceImpl;
import com.hamroschool.service.impl.TeacherServiceImpl;
import com.hamroschool.util.SceneSwitcher;
import com.hamroschool.util.SessionContext;
import com.hamroschool.util.Utils;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class StudentDashboardController {

    // ── Services ──────────────────────────────────────────────────────────────
    private final MarkService       markService       = MarkServiceImpl.getInstance();
    private final AttendanceService attendanceService = AttendanceServiceImpl.getInstance();
    private final TeacherService    teacherService    = TeacherServiceImpl.getInstance();

    private String studentUsername;

    // ── Cache — populated ONCE in background thread ────────────────────────
    private volatile List<Mark>          cachedMarks          = List.of();
    private volatile Map<String, String> cachedSubjectTeacher = Map.of();
    private volatile boolean             dataLoaded           = false;
    
    // ── Pre-computed aggregations for instant access ─────────────────────────
    private volatile Map<String, List<Mark>> marksBySubject   = Map.of();
    private volatile List<AttendanceRow>     attendanceRows   = List.of();
    private volatile double                  cachedAvgGrade   = 0.0;
    private volatile double                  cachedAvgAtt     = 0.0;

    // ── Row models ────────────────────────────────────────────────────────────
    public record CourseRow(String subject, String teacher, double avgPct, String grade) {}
    public record AttendanceRow(String subject, String teacher, double pct) {}

    // ── Pagination ────────────────────────────────────────────────────────────
    private static final int PAGE_SIZE = 5;
    private final ObservableList<CourseRow> allCourses = FXCollections.observableArrayList();
    private List<CourseRow> filteredCourses = List.of();
    private int currentPage = 0;

    // ── FXML injections ───────────────────────────────────────────────────────
    @FXML private Label  welcomeSubLabel;
    @FXML private Label  userInitialsLabel;
    @FXML private Label  userNameLabel;
    @FXML private Button logoutButton;

    @FXML private Button navDashboardBtn;
    @FXML private Button navGradesBtn;
    @FXML private Button navAttendanceBtn;

    // stat cards
    @FXML private Label statSubjectsLabel;
    @FXML private Label statGradeLabel;
    @FXML private Label statAttLabel;

    // panes
    @FXML private VBox dashboardPane;
    @FXML private VBox gradesPane;
    @FXML private VBox attendancePane;

    // courses table
    @FXML private TextField                      searchField;
    @FXML private TableView<CourseRow>           coursesTable;
    @FXML private TableColumn<CourseRow, String> cColCourse;
    @FXML private TableColumn<CourseRow, String> cColInstructor;
    @FXML private TableColumn<CourseRow, String> cColGrade;
    @FXML private Label                          coursesSummaryLabel;
    @FXML private Button                         prevButton;
    @FXML private Button                         nextButton;

    // grades table
    @FXML private TableView<Mark>           marksTable;
    @FXML private TableColumn<Mark, String> mColSubject;
    @FXML private TableColumn<Mark, String> mColTeacher;
    @FXML private TableColumn<Mark, String> mColExam;
    @FXML private TableColumn<Mark, String> mColScore;
    @FXML private TableColumn<Mark, String> mColGrade;
    @FXML private TableColumn<Mark, String> mColRemarks;
    @FXML private Label                     marksSummaryLabel;

    // attendance table
    @FXML private TableView<AttendanceRow>           attTable;
    @FXML private TableColumn<AttendanceRow, String> aColSubject;
    @FXML private TableColumn<AttendanceRow, String> aColTeacher;
    @FXML private TableColumn<AttendanceRow, String> aColPct;
    @FXML private TableColumn<AttendanceRow, String> aColStatus;
    @FXML private Label                              attSummaryLabel;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        studentUsername = SessionContext.getInstance().requireCurrentUser().getUsername();
        String displayName = Utils.formatName(studentUsername);
        welcomeSubLabel.setText("Welcome back, " + displayName + " — here's your learning overview");
        userInitialsLabel.setText(Utils.initials(studentUsername));
        userNameLabel.setText(displayName);

        setupCoursesTable();
        setupGradesTable();
        setupAttendanceTable();

        searchField.textProperty().addListener((obs, o, n) -> { currentPage = 0; applyFilter(n); });

        showDashboard();

        // Load ALL MongoDB data once on a background thread — UI never blocks
        Thread loader = new Thread(() -> {
            try {
                long startTime = System.currentTimeMillis();
                
                List<Mark> marks = markService.getMarksByStudent(studentUsername);

                java.util.LinkedHashMap<String, String> subTeach = new java.util.LinkedHashMap<>();
                java.util.LinkedHashMap<String, Double> attMap   = new java.util.LinkedHashMap<>();

                for (var teacher : teacherService.getAllTeachers()) {
                    String tName   = teacher.getUsername();
                    String subject = teacherService.getSubject(tName).orElse(null);
                    if (subject == null || subject.isBlank()) continue;
                    subTeach.putIfAbsent(subject, tName);
                    Double pct = attendanceService
                            .getAttendancePercentages(tName, subject)
                            .get(studentUsername);
                    if (pct != null) attMap.put(subject, pct);
                }

                // Pre-compute aggregations
                Map<String, List<Mark>> bySubject = marks.stream()
                        .filter(m -> m.getSubjectName() != null && !m.getSubjectName().isBlank())
                        .collect(Collectors.groupingBy(Mark::getSubjectName));
                
                double avgGrade = marks.isEmpty() ? -1
                        : marks.stream().mapToDouble(Mark::getPercentage).average().orElse(-1);
                
                double avgAtt = attMap.isEmpty() ? -1
                        : attMap.values().stream().mapToDouble(Double::doubleValue).average().orElse(-1);
                
                List<AttendanceRow> attRows = new ArrayList<>();
                for (Map.Entry<String, Double> e : attMap.entrySet()) {
                    String subject = e.getKey();
                    String teacher = subTeach.getOrDefault(subject, "—");
                    attRows.add(new AttendanceRow(subject, teacher, e.getValue()));
                }

                cachedMarks          = marks;
                cachedSubjectTeacher = subTeach;
                marksBySubject       = bySubject;
                cachedAvgGrade       = avgGrade;
                cachedAvgAtt         = avgAtt;
                attendanceRows       = attRows;
                dataLoaded           = true;

                long loadTime = System.currentTimeMillis() - startTime;
                if (loadTime > 100) {
                    System.out.println("[StudentDashboard] Data loaded in " + loadTime + "ms");
                }

                javafx.application.Platform.runLater(() -> {
                    refreshStats();
                    loadCourses();
                });
            } catch (Exception ex) {
                System.err.println("[StudentDashboard] Data load error: " + ex.getMessage());
            }
        }, "StudentDataLoader");
        loader.setDaemon(true);
        loader.start();
    }

    // ── Nav handlers ──────────────────────────────────────────────────────────

    @FXML private void handleNavDashboard()  { showDashboard(); }
    @FXML private void handleNavGrades()     { showGrades(); }
    @FXML private void handleNavAttendance() { showAttendance(); }

    @FXML
    private void handleLogout() {
        SessionContext.getInstance().clear();
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/hello-view.fxml", "Hamro School", 920, 720);
    }

    @FXML private void handlePrevPage() {
        if (currentPage > 0) { currentPage--; renderPage(); }
    }

    @FXML private void handleNextPage() {
        int total = (int) Math.ceil((double) filteredCourses.size() / PAGE_SIZE);
        if (currentPage < total - 1) { currentPage++; renderPage(); }
    }

    // ── Pane switching ────────────────────────────────────────────────────────

    private void showDashboard() {
        setPane(dashboardPane);
        setActiveNav(navDashboardBtn);
        if (dataLoaded) { refreshStats(); loadCourses(); }
    }

    private void showGrades() {
        setPane(gradesPane);
        setActiveNav(navGradesBtn);
        loadMarks();
    }

    private void showAttendance() {
        setPane(attendancePane);
        setActiveNav(navAttendanceBtn);
        loadAttendance();
    }

    private void setPane(VBox target) {
        for (VBox p : List.of(dashboardPane, gradesPane, attendancePane)) {
            p.setVisible(p == target);
            p.setManaged(p == target);
        }
    }

    private void setActiveNav(Button active) {
        String on  = "-fx-background-color: #111111; -fx-background-radius: 8; -fx-text-fill: white; "
                   + "-fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 0 12 0 12; -fx-cursor: hand;";
        String off = "-fx-background-color: transparent; -fx-background-radius: 8; -fx-text-fill: #44403c; "
                   + "-fx-font-size: 13px; -fx-font-weight: 500; -fx-padding: 0 12 0 12; -fx-cursor: hand;";
        for (Button b : List.of(navDashboardBtn, navGradesBtn, navAttendanceBtn))
            b.setStyle(b == active ? on : off);
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    private void refreshStats() {
        if (!dataLoaded) return;

        // Subjects = union of assigned subjects + subjects with marks
        java.util.LinkedHashSet<String> subjects = new java.util.LinkedHashSet<>(cachedSubjectTeacher.keySet());
        subjects.addAll(marksBySubject.keySet());
        statSubjectsLabel.setText(String.valueOf(subjects.size()));

        // Use pre-computed average grade
        statGradeLabel.setText(cachedAvgGrade >= 0 ? gradeFromPct(cachedAvgGrade) : "—");

        // Use pre-computed average attendance
        statAttLabel.setText(cachedAvgAtt >= 0 ? String.format("%.0f%%", cachedAvgAtt) : "—");
    }

    // ── Courses table ─────────────────────────────────────────────────────────

    private void loadCourses() {
        if (!dataLoaded) return;

        java.util.LinkedHashSet<String> allSubjects = new java.util.LinkedHashSet<>(cachedSubjectTeacher.keySet());
        allSubjects.addAll(marksBySubject.keySet());

        List<CourseRow> rows = new ArrayList<>();
        for (String subject : allSubjects) {
            List<Mark> marks = marksBySubject.getOrDefault(subject, List.of());
            double avg    = marks.isEmpty() ? 0 : marks.stream().mapToDouble(Mark::getPercentage).average().orElse(0);
            String teacher = marks.isEmpty() ? cachedSubjectTeacher.getOrDefault(subject, "—") : marks.get(0).getTeacherUsername();
            String grade   = marks.isEmpty() ? "—" : gradeFromPct(avg);
            rows.add(new CourseRow(subject, teacher, avg, grade));
        }

        allCourses.setAll(rows);
        currentPage = 0;
        applyFilter(searchField.getText());
    }

    private void applyFilter(String query) {
        if (!dataLoaded) return;
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        filteredCourses = q.isBlank() ? new ArrayList<>(allCourses)
                : allCourses.stream()
                    .filter(r -> r.subject().toLowerCase(Locale.ROOT).contains(q)
                              || r.teacher().toLowerCase(Locale.ROOT).contains(q))
                    .toList();
        renderPage();
    }

    private void renderPage() {
        if (!dataLoaded || filteredCourses.isEmpty()) {
            coursesTable.setItems(FXCollections.emptyObservableList());
            coursesSummaryLabel.setText("Showing 0 of 0 courses");
            prevButton.setDisable(true);
            nextButton.setDisable(true);
            return;
        }
        int total = (int) Math.ceil((double) filteredCourses.size() / PAGE_SIZE);
        if (currentPage >= total) currentPage = total - 1;
        int from = currentPage * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, filteredCourses.size());
        coursesTable.setItems(FXCollections.observableArrayList(filteredCourses.subList(from, to)));
        coursesSummaryLabel.setText(
                "Showing " + (from + 1) + "–" + to + " of " + filteredCourses.size() + " courses");
        prevButton.setDisable(currentPage == 0);
        nextButton.setDisable(currentPage >= total - 1);
    }

    private void setupCoursesTable() {
        // Course: icon + name
        cColCourse.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().subject()));
        cColCourse.setCellFactory(col -> new TableCell<CourseRow, String>() {
            @Override protected void updateItem(String subject, boolean empty) {
                super.updateItem(subject, empty);
                if (empty || subject == null) { setGraphic(null); return; }
                Label icon = new Label(subjectIcon(subject));
                icon.setStyle("-fx-font-size: 15px; -fx-min-width: 26;");
                Label name = new Label(subject);
                name.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #111111;");
                HBox box = new HBox(10, icon, name);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box); setText(null);
            }
        });

        // Instructor
        cColInstructor.setCellValueFactory(c -> new ReadOnlyStringWrapper(Utils.formatName(c.getValue().teacher())));
        cColInstructor.setCellFactory(col -> plainCell("#44403c", false));

        // Grade badge
        cColGrade.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().grade()));
        cColGrade.setCellFactory(col -> new TableCell<CourseRow, String>() {
            @Override protected void updateItem(String grade, boolean empty) {
                super.updateItem(grade, empty);
                if (empty || grade == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(grade);
                badge.setStyle("-fx-background-color: #f5f5f4; -fx-border-color: #e7e5e4; "
                        + "-fx-border-radius: 6; -fx-background-radius: 6; "
                        + "-fx-text-fill: #111111; -fx-font-size: 12px; -fx-font-weight: 700; "
                        + "-fx-padding: 3 10 3 10;");
                setGraphic(badge); setText(null);
            }
        });

        coursesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        coursesTable.setPlaceholder(new Label("No courses yet."));
        coursesTable.setStyle("-fx-background-color: transparent;");
    }

    // ── Grades table ──────────────────────────────────────────────────────────

    private void loadMarks() {
        if (!dataLoaded) return;
        marksTable.setItems(FXCollections.observableArrayList(cachedMarks));
        marksSummaryLabel.setText(cachedMarks.isEmpty()
                ? "No marks recorded yet."
                : "Showing " + cachedMarks.size() + " record" + (cachedMarks.size() == 1 ? "" : "s"));
    }

    private void setupGradesTable() {
        mColSubject.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getSubjectName()));
        mColSubject.setCellFactory(col -> plainCell("#111111", true));

        mColTeacher.setCellValueFactory(c -> new ReadOnlyStringWrapper(Utils.formatName(c.getValue().getTeacherUsername())));
        mColTeacher.setCellFactory(col -> plainCell("#44403c", false));

        mColExam.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getExamType()));
        mColExam.setCellFactory(col -> plainCell("#44403c", false));

        mColScore.setCellValueFactory(c -> {
            Mark m = c.getValue();
            return new ReadOnlyStringWrapper(m.getScore() + " / " + m.getFullMarks()
                    + "  (" + m.getPercentage() + "%)");
        });
        mColScore.setCellFactory(col -> plainCell("#111111", true));

        mColGrade.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getGrade()));
        mColGrade.setCellFactory(col -> new TableCell<Mark, String>() {
            @Override protected void updateItem(String grade, boolean empty) {
                super.updateItem(grade, empty);
                if (empty || grade == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(grade);
                badge.setStyle("-fx-background-color: #f5f5f4; -fx-border-color: #e7e5e4; "
                        + "-fx-border-radius: 6; -fx-background-radius: 6; "
                        + "-fx-text-fill: #111111; -fx-font-size: 12px; -fx-font-weight: 700; "
                        + "-fx-padding: 3 10 3 10;");
                setGraphic(badge); setText(null);
            }
        });

        mColRemarks.setCellValueFactory(c -> {
            String r = c.getValue().getRemarks();
            return new ReadOnlyStringWrapper(r == null || r.isBlank() ? "—" : r);
        });
        mColRemarks.setCellFactory(col -> plainCell("#78716c", false));

        marksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        marksTable.setPlaceholder(new Label("No marks recorded yet."));
        marksTable.setStyle("-fx-background-color: transparent;");
    }

    // ── Attendance table ──────────────────────────────────────────────────────

    private void loadAttendance() {
        if (!dataLoaded) return;
        attTable.setItems(FXCollections.observableArrayList(attendanceRows));
        attSummaryLabel.setText(attendanceRows.isEmpty()
                ? "No attendance records found."
                : "Showing " + attendanceRows.size() + " subject" + (attendanceRows.size() == 1 ? "" : "s"));
    }

    private void setupAttendanceTable() {
        aColSubject.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().subject()));
        aColSubject.setCellFactory(col -> plainCell("#111111", true));

        aColTeacher.setCellValueFactory(c -> new ReadOnlyStringWrapper(Utils.formatName(c.getValue().teacher())));
        aColTeacher.setCellFactory(col -> plainCell("#44403c", false));

        aColPct.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                String.format("%.1f%%", c.getValue().pct())));
        aColPct.setCellFactory(col -> new TableCell<AttendanceRow, String>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                double pct = Double.parseDouble(v.replace("%", ""));
                String color = pct >= 75 ? "#16a34a" : pct >= 50 ? "#d97706" : "#dc2626";
                setText(v);
                setStyle("-fx-font-weight: 700; -fx-font-size: 13px; -fx-text-fill: " + color + ";");
            }
        });

        aColStatus.setCellValueFactory(c -> {
            double p = c.getValue().pct();
            return new ReadOnlyStringWrapper(p >= 75 ? "Good" : p >= 50 ? "At Risk" : "Critical");
        });
        aColStatus.setCellFactory(col -> new TableCell<AttendanceRow, String>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(s);
                String bg, fg;
                switch (s) {
                    case "Good"    -> { bg = "#dcfce7"; fg = "#16a34a"; }
                    case "At Risk" -> { bg = "#fef9c3"; fg = "#a16207"; }
                    default        -> { bg = "#fee2e2"; fg = "#dc2626"; }
                }
                badge.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg
                        + "; -fx-padding:4 12 4 12; -fx-background-radius:999; "
                        + "-fx-font-size:12px; -fx-font-weight:700;");
                setGraphic(badge); setText(null);
            }
        });

        attTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        attTable.setPlaceholder(new Label("No attendance records found."));
        attTable.setStyle("-fx-background-color: transparent;");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private <T> TableCell<T, String> plainCell(String color, boolean bold) {
        return new TableCell<T, String>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(v);
                setStyle("-fx-font-size: 13px; -fx-font-weight: " + (bold ? "600" : "400")
                        + "; -fx-text-fill: " + color + ";");
            }
        };
    }

    private String subjectIcon(String subject) {
        if (subject == null) return "📚";
        String s = subject.toLowerCase(Locale.ROOT);
        if (s.contains("math"))                  return "Σ";
        if (s.contains("chem"))                  return "⚗";
        if (s.contains("phys"))                  return "⚛";
        if (s.contains("hist"))                  return "🏛";
        if (s.contains("eng"))                   return "📝";
        if (s.contains("geo"))                   return "🌐";
        if (s.contains("bio"))                   return "🧬";
        if (s.contains("comp") || s.contains("it")) return "💻";
        if (s.contains("sci"))                   return "🔬";
        return "📚";
    }

    private String gradeFromPct(double pct) {
        if (pct >= 90) return "A+";
        if (pct >= 80) return "A";
        if (pct >= 70) return "B+";
        if (pct >= 60) return "B";
        if (pct >= 50) return "C";
        if (pct >= 40) return "D";
        return "F";
    }

}
