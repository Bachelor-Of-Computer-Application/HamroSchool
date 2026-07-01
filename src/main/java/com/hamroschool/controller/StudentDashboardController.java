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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class StudentDashboardController {

    private final MarkService       markService       = MarkServiceImpl.getInstance();
    private final AttendanceService attendanceService = AttendanceServiceImpl.getInstance();
    private final TeacherService    teacherService    = TeacherServiceImpl.getInstance();

    private String studentUsername;

    public record CourseRow(String subject, String teacher, double avgPct, String grade) {}

    public record AttendanceRow(String subject, String teacher, double pct) {}

    private static final int PAGE_SIZE = 5;
    private final ObservableList<CourseRow> allCourses = FXCollections.observableArrayList();
    private List<CourseRow> filteredCourses = List.of();
    private int currentPage = 0;

    @FXML private Label  welcomeSubLabel;
    @FXML private Label  userInitialsLabel;
    @FXML private Label  userNameLabel;
    @FXML private Button logoutButton;

    @FXML private Button navDashboardBtn;
    @FXML private Button navGradesBtn;
    @FXML private Button navAttendanceBtn;

    @FXML private Label statSubjectsLabel;
    @FXML private Label statGradeLabel;
    @FXML private Label statAttLabel;

    @FXML private VBox dashboardPane;
    @FXML private VBox gradesPane;
    @FXML private VBox attendancePane;

    @FXML private TextField              searchField;
    @FXML private TableView<CourseRow>   coursesTable;
    @FXML private TableColumn<CourseRow, String> cColCourse;
    @FXML private TableColumn<CourseRow, String> cColInstructor;
    @FXML private TableColumn<CourseRow, String> cColProgress;
    @FXML private TableColumn<CourseRow, String> cColGrade;
    @FXML private Label                  coursesSummaryLabel;
    @FXML private Button                 prevButton;
    @FXML private Button                 nextButton;

    @FXML private TableView<Mark>         marksTable;
    @FXML private TableColumn<Mark, String> mColSubject;
    @FXML private TableColumn<Mark, String> mColTeacher;
    @FXML private TableColumn<Mark, String> mColExam;
    @FXML private TableColumn<Mark, String> mColScore;
    @FXML private TableColumn<Mark, String> mColGrade;
    @FXML private TableColumn<Mark, String> mColRemarks;
    @FXML private Label                   marksSummaryLabel;

    @FXML private TableView<AttendanceRow>          attTable;
    @FXML private TableColumn<AttendanceRow, String> aColSubject;
    @FXML private TableColumn<AttendanceRow, String> aColTeacher;
    @FXML private TableColumn<AttendanceRow, String> aColPct;
    @FXML private TableColumn<AttendanceRow, String> aColStatus;
    @FXML private Label                              attSummaryLabel;


    @FXML
    public void initialize() {
        studentUsername = SessionContext.getInstance().requireCurrentUser().getUsername();

        String displayName = fmt(studentUsername);
        welcomeSubLabel.setText("Welcome back, " + displayName + " — here's your learning overview");
        userInitialsLabel.setText(initials(studentUsername));
        userNameLabel.setText(displayName);

        setupCoursesTable();
        setupGradesTable();
        setupAttendanceTable();

        searchField.textProperty().addListener((obs, o, n) -> {
            currentPage = 0;
            applyFilter(n);
        });

        showDashboard();
    }


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


    private void showDashboard() {
        setPane(dashboardPane);
        setActiveNav(navDashboardBtn);
        loadCourses();
        refreshStats();
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
        String on  = "-fx-background-color: #111111; -fx-background-radius: 8; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 0 12 0 12; -fx-cursor: hand;";
        String off = "-fx-background-color: transparent; -fx-background-radius: 8; -fx-text-fill: #44403c; -fx-font-size: 13px; -fx-font-weight: 500; -fx-padding: 0 12 0 12; -fx-cursor: hand;";
        for (Button b : List.of(navDashboardBtn, navGradesBtn, navAttendanceBtn))
            b.setStyle(b == active ? on : off);
    }


    private void refreshStats() {
        List<Mark> allMarks = markService.getMarksByStudent(studentUsername);

        long subjectCount = allMarks.stream().map(Mark::getSubjectName).distinct().count();
        statSubjectsLabel.setText(subjectCount > 0 ? String.valueOf(subjectCount) : "0");

        double avg = allMarks.stream().mapToDouble(Mark::getPercentage).average().orElse(-1);
        statGradeLabel.setText(avg >= 0 ? gradeFromPct(avg) : "—");

        double attPct = computeAvgAttendance();
        statAttLabel.setText(attPct >= 0 ? String.format("%.0f%%", attPct) : "—");
    }

    private double computeAvgAttendance() {
        double total = 0; int count = 0;
        for (var teacher : teacherService.getAllTeachers()) {
            String tName   = teacher.getUsername();
            String subject = teacherService.getSubject(tName).orElse(null);
            if (subject == null) continue;
            Map<String, Double> pctMap = attendanceService.getAttendancePercentages(tName, subject);
            Double pct = pctMap.get(studentUsername);
            if (pct != null) { total += pct; count++; }
        }
        return count > 0 ? total / count : -1;
    }


    private void loadCourses() {
        List<Mark> allMarks = markService.getMarksByStudent(studentUsername);

        Map<String, List<Mark>> bySubject = allMarks.stream()
                .collect(Collectors.groupingBy(Mark::getSubjectName));

        List<CourseRow> rows = new ArrayList<>();
        for (Map.Entry<String, List<Mark>> entry : bySubject.entrySet()) {
            String subject = entry.getKey();
            List<Mark> marks = entry.getValue();
            double avg = marks.stream().mapToDouble(Mark::getPercentage).average().orElse(0);
            String teacher = marks.isEmpty() ? "—" : marks.get(0).getTeacherUsername();
            rows.add(new CourseRow(subject, teacher, avg, gradeFromPct(avg)));
        }

        allCourses.setAll(rows);
        currentPage = 0;
        applyFilter(searchField.getText());
    }

    private void applyFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        filteredCourses = q.isBlank() ? new ArrayList<>(allCourses)
                : allCourses.stream()
                    .filter(r -> r.subject().toLowerCase(Locale.ROOT).contains(q)
                              || r.teacher().toLowerCase(Locale.ROOT).contains(q))
                    .toList();
        renderPage();
    }

    private void renderPage() {
        int total = Math.max(1, (int) Math.ceil((double) filteredCourses.size() / PAGE_SIZE));
        if (currentPage >= total) currentPage = total - 1;
        int from = currentPage * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, filteredCourses.size());
        coursesTable.setItems(FXCollections.observableArrayList(filteredCourses.subList(from, to)));
        coursesSummaryLabel.setText("Showing " + (from + 1) + "–" + to + " of " + filteredCourses.size() + " courses");
        prevButton.setDisable(currentPage == 0);
        nextButton.setDisable(currentPage >= total - 1);
    }

    private void setupCoursesTable() {
        cColCourse.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().subject()));
        cColCourse.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String subject, boolean empty) {
                super.updateItem(subject, empty);
                if (empty || subject == null) { setGraphic(null); return; }
                Label icon = new Label(subjectIcon(subject));
                icon.setStyle("-fx-font-size: 15px; -fx-min-width: 28;");
                Label name = new Label(subject);
                name.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #111111;");
                HBox box = new HBox(10, icon, name);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box); setText(null);
            }
        });

        cColInstructor.setCellValueFactory(c -> new ReadOnlyStringWrapper("Mr/Ms. " + fmt(c.getValue().teacher())));
        cColInstructor.setCellFactory(col -> plainCell("#44403c", false));

        cColProgress.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.valueOf(c.getValue().avgPct())));
        cColProgress.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setGraphic(null); return; }
                double pct = Double.parseDouble(val);
                double clampedPct = Math.min(100, Math.max(0, pct));

                StackPane track = new StackPane();
                track.setPrefHeight(8);
                track.setStyle("-fx-background-color: #e7e5e4; -fx-background-radius: 999;");

                javafx.scene.layout.Region fill = new javafx.scene.layout.Region();
                fill.setPrefHeight(8);
                fill.setStyle("-fx-background-color: #111111; -fx-background-radius: 999;");

                HBox bar = new HBox();
                bar.setPrefHeight(8);
                bar.setMaxWidth(160);
                bar.setStyle("-fx-background-color: #e7e5e4; -fx-background-radius: 999;");

                javafx.scene.layout.Region filled = new javafx.scene.layout.Region();
                filled.setPrefHeight(8);
                filled.setPrefWidth(160 * clampedPct / 100.0);
                filled.setStyle("-fx-background-color: #111111; -fx-background-radius: 999;");

                javafx.scene.layout.Region empty2 = new javafx.scene.layout.Region();
                empty2.setPrefHeight(8);
                empty2.setPrefWidth(160 * (1 - clampedPct / 100.0));
                empty2.setStyle("-fx-background-color: #e7e5e4; -fx-background-radius: 999;");

                bar.getChildren().addAll(filled, empty2);
                setGraphic(bar); setText(null);
            }
        });

        cColGrade.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().grade()));
        cColGrade.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String grade, boolean empty) {
                super.updateItem(grade, empty);
                if (empty || grade == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(grade);
                badge.setStyle(
                    "-fx-background-color: #f5f5f4; -fx-border-color: #e7e5e4; " +
                    "-fx-border-radius: 6; -fx-background-radius: 6; " +
                    "-fx-text-fill: #111111; -fx-font-size: 12px; -fx-font-weight: 700; " +
                    "-fx-padding: 3 10 3 10;");
                setGraphic(badge); setText(null);
            }
        });

        coursesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        coursesTable.setPlaceholder(new Label("No courses found."));
        coursesTable.setStyle("-fx-background-color: transparent;");
    }


    private void setupGradesTable() {
        mColSubject.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getSubjectName()));
        mColSubject.setCellFactory(col -> plainCell("#111111", true));

        mColTeacher.setCellValueFactory(c -> new ReadOnlyStringWrapper(fmt(c.getValue().getTeacherUsername())));
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
        mColGrade.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String grade, boolean empty) {
                super.updateItem(grade, empty);
                if (empty || grade == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(grade);
                badge.setStyle("-fx-background-color: #f5f5f4; -fx-border-color: #e7e5e4; " +
                    "-fx-border-radius: 6; -fx-background-radius: 6; " +
                    "-fx-text-fill: #111111; -fx-font-size: 12px; -fx-font-weight: 700; " +
                    "-fx-padding: 3 10 3 10;");
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

    private void loadMarks() {
        List<Mark> marks = markService.getMarksByStudent(studentUsername);
        marksTable.setItems(FXCollections.observableArrayList(marks));
        marksSummaryLabel.setText(marks.isEmpty()
                ? "No marks recorded yet."
                : "Showing " + marks.size() + " record" + (marks.size() == 1 ? "" : "s"));
    }


    private void setupAttendanceTable() {
        aColSubject.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().subject()));
        aColSubject.setCellFactory(col -> plainCell("#111111", true));

        aColTeacher.setCellValueFactory(c -> new ReadOnlyStringWrapper(fmt(c.getValue().teacher())));
        aColTeacher.setCellFactory(col -> plainCell("#44403c", false));

        aColPct.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                String.format("%.1f%%", c.getValue().pct())));
        aColPct.setCellFactory(col -> new TableCell<>() {
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
        aColStatus.setCellFactory(col -> new TableCell<>() {
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

    private void loadAttendance() {
        List<AttendanceRow> rows = new ArrayList<>();
        for (var teacher : teacherService.getAllTeachers()) {
            String tName   = teacher.getUsername();
            String subject = teacherService.getSubject(tName).orElse(null);
            if (subject == null) continue;
            Map<String, Double> pctMap = attendanceService.getAttendancePercentages(tName, subject);
            Double pct = pctMap.get(studentUsername);
            if (pct != null) rows.add(new AttendanceRow(subject, tName, pct));
        }
        attTable.setItems(FXCollections.observableArrayList(rows));
        attSummaryLabel.setText(rows.isEmpty()
                ? "No attendance records found."
                : "Showing " + rows.size() + " subject" + (rows.size() == 1 ? "" : "s"));
    }


    private <T> TableCell<T, String> plainCell(String color, boolean bold) {
        return new TableCell<>() {
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
        if (s.contains("math"))    return "Σ";
        if (s.contains("chem"))    return "⚗";
        if (s.contains("phys"))    return "⚛";
        if (s.contains("hist"))    return "🏛";
        if (s.contains("eng"))     return "📝";
        if (s.contains("geo"))     return "🌐";
        if (s.contains("bio"))     return "🧬";
        if (s.contains("comp") || s.contains("it")) return "💻";
        if (s.contains("sci"))     return "🔬";
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

    private String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] p = name.trim().split("\\s+");
        return p.length == 1
                ? p[0].substring(0, Math.min(2, p[0].length())).toUpperCase(Locale.ROOT)
                : (p[0].substring(0, 1) + p[1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private String fmt(String username) {
        if (username == null || username.isBlank()) return "Unknown";
        String t = username.trim();
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }
}
