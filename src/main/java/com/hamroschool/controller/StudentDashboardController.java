package com.hamroschool.controller;

import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class StudentDashboardController {

    private final MarkService       markService       = MarkServiceImpl.getInstance();
    private final AttendanceService attendanceService = AttendanceServiceImpl.getInstance();
    private final TeacherService    teacherService    = TeacherServiceImpl.getInstance();

    private String studentUsername;

    @FXML private Label  welcomeLabel;
    @FXML private Label  welcomeSubLabel;
    @FXML private Label  userInitialsLabel;
    @FXML private Label  userNameLabel;
    @FXML private Button logoutButton;

    @FXML private Button navDashboardBtn;
    @FXML private Button navMarksBtn;
    @FXML private Button navAttendanceBtn;

    @FXML private HBox  dashboardPane;
    @FXML private Label statSubjectsLabel;
    @FXML private Label statAvgLabel;
    @FXML private Label statAttLabel;
    @FXML private Label statGradeLabel;

    @FXML private VBox                    marksPane;
    @FXML private TableView<Mark>         marksTable;
    @FXML private TableColumn<Mark, String> mColSubject;
    @FXML private TableColumn<Mark, String> mColTeacher;
    @FXML private TableColumn<Mark, String> mColExam;
    @FXML private TableColumn<Mark, String> mColScore;
    @FXML private TableColumn<Mark, String> mColGrade;
    @FXML private TableColumn<Mark, String> mColRemarks;
    @FXML private Label                   marksSummaryLabel;

    @FXML private VBox                         attendancePane;
    @FXML private TableView<AttendanceRow>     attTable;
    @FXML private TableColumn<AttendanceRow, String> aColSubject;
    @FXML private TableColumn<AttendanceRow, String> aColTeacher;
    @FXML private TableColumn<AttendanceRow, String> aColPct;
    @FXML private TableColumn<AttendanceRow, String> aColStatus;
    @FXML private Label                        attSummaryLabel;


    @FXML
    public void initialize() {
        studentUsername = SessionContext.getInstance().requireCurrentUser().getUsername();

        String displayName = fmt(studentUsername);
        welcomeLabel.setText("Hello, " + displayName + " 👋");
        welcomeSubLabel.setText("Here's your academic summary for today.");
        userInitialsLabel.setText(initials(studentUsername));
        userNameLabel.setText(studentUsername);

        setupMarksTable();
        setupAttendanceTable();

        showMarksPane();
        refreshStats();
    }


    @FXML
    private void handleNavDashboard() {
        showMarksPane();
        refreshStats();
    }

    @FXML
    private void handleNavMarks() {
        showMarksPane();
    }

    @FXML
    private void handleNavAttendance() {
        showAttendancePane();
    }

    @FXML
    private void handleLogout() {
        SessionContext.getInstance().clear();
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/hello-view.fxml", "Hamro School", 920, 720);
    }


    private void showMarksPane() {
        marksPane.setVisible(true);
        marksPane.setManaged(true);
        attendancePane.setVisible(false);
        attendancePane.setManaged(false);
        dashboardPane.setVisible(true);
        dashboardPane.setManaged(true);
        setActiveNav(navMarksBtn);
        loadMarks();
    }

    private void showAttendancePane() {
        marksPane.setVisible(false);
        marksPane.setManaged(false);
        attendancePane.setVisible(true);
        attendancePane.setManaged(true);
        dashboardPane.setVisible(false);
        dashboardPane.setManaged(false);
        setActiveNav(navAttendanceBtn);
        loadAttendance();
    }

    private void setActiveNav(Button active) {
        String activeStyle   = "-fx-background-color: #111111; -fx-background-radius: 8; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 700; -fx-padding: 10 14 10 14; -fx-cursor: hand;";
        String inactiveStyle = "-fx-background-color: transparent; -fx-background-radius: 8; -fx-text-fill: #272727; -fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 10 14 10 14; -fx-cursor: hand;";
        for (Button btn : List.of(navDashboardBtn, navMarksBtn, navAttendanceBtn)) {
            btn.setStyle(btn == active ? activeStyle : inactiveStyle);
        }
    }


    private void refreshStats() {
        List<Mark> allMarks = markService.getMarksByStudent(studentUsername);

        long subjects = allMarks.stream().map(Mark::getSubjectName).distinct().count();
        statSubjectsLabel.setText(subjects > 0 ? String.valueOf(subjects) : "—");

        double avg = allMarks.stream().mapToDouble(Mark::getPercentage).average().orElse(-1);
        statAvgLabel.setText(avg >= 0 ? String.format("%.0f%%", avg) : "—");

        statGradeLabel.setText(avg >= 0 ? gradeFromPct(avg) : "—");

        List<String> assignedTeachers = teacherService.getAllTeachers()
                .stream().map(t -> t.getUsername()).toList();
        double totalPct = 0;
        int count = 0;
        for (String teacher : assignedTeachers) {
            Map<String, Double> pctMap = attendanceService.getAttendancePercentages(teacher,
                    teacherService.getSubject(teacher).orElse("General"));
            Double pct = pctMap.get(studentUsername);
            if (pct != null) { totalPct += pct; count++; }
        }
        statAttLabel.setText(count > 0 ? String.format("%.0f%%", totalPct / count) : "—");
    }


    private void setupMarksTable() {
        mColSubject.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getSubjectName()));
        mColSubject.setCellFactory(col -> styledCell("#222222", true));

        mColTeacher.setCellValueFactory(c -> new ReadOnlyStringWrapper(fmt(c.getValue().getTeacherUsername())));
        mColTeacher.setCellFactory(col -> styledCell("#555555", false));

        mColExam.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getExamType()));
        mColExam.setCellFactory(col -> styledCell("#555555", false));

        mColScore.setCellValueFactory(c -> {
            Mark m = c.getValue();
            return new ReadOnlyStringWrapper(m.getScore() + " / " + m.getFullMarks()
                    + "  (" + m.getPercentage() + "%)");
        });
        mColScore.setCellFactory(col -> styledCell("#f97316", true));

        mColGrade.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getGrade()));
        mColGrade.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String grade, boolean empty) {
                super.updateItem(grade, empty);
                if (empty || grade == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(grade);
                badge.setStyle("-fx-background-color: " + gradeBadgeColor(grade) + "; " +
                        "-fx-text-fill: white; -fx-padding: 4 12 4 12; " +
                        "-fx-background-radius: 999; -fx-font-size: 12px; -fx-font-weight: 800;");
                setGraphic(badge); setText(null);
            }
        });

        mColRemarks.setCellValueFactory(c -> {
            String r = c.getValue().getRemarks();
            return new ReadOnlyStringWrapper(r == null || r.isBlank() ? "—" : r);
        });
        mColRemarks.setCellFactory(col -> styledCell("#888888", false));

        marksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        marksTable.setPlaceholder(new Label("No marks recorded yet."));
        marksTable.setStyle("-fx-background-color: transparent;");
    }

    private void loadMarks() {
        List<Mark> marks = markService.getMarksByStudent(studentUsername);
        marksTable.setItems(FXCollections.observableArrayList(marks));
        marksSummaryLabel.setText(marks.isEmpty()
                ? "No marks recorded yet."
                : "Showing " + marks.size() + " mark" + (marks.size() == 1 ? "" : "s"));
    }


    public record AttendanceRow(String subject, String teacher, double pct) {}

    private void setupAttendanceTable() {
        aColSubject.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().subject()));
        aColSubject.setCellFactory(col -> styledCell("#222222", true));

        aColTeacher.setCellValueFactory(c -> new ReadOnlyStringWrapper(fmt(c.getValue().teacher())));
        aColTeacher.setCellFactory(col -> styledCell("#555555", false));

        aColPct.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().pct() + "%"));
        aColPct.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                double pct = Double.parseDouble(v.replace("%", ""));
                String color = pct >= 75 ? "#16a34a" : pct >= 50 ? "#d97706" : "#dc2626";
                setText(v);
                setStyle("-fx-font-weight: 700; -fx-font-size: 13px; -fx-text-fill: " + color
                        + "; -fx-alignment: center;");
            }
        });

        aColStatus.setCellValueFactory(c -> {
            double pct = c.getValue().pct();
            String status = pct >= 75 ? "Good" : pct >= 50 ? "At Risk" : "Critical";
            return new ReadOnlyStringWrapper(status);
        });
        aColStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(status);
                String bg, fg;
                switch (status) {
                    case "Good"     -> { bg = "#dcfce7"; fg = "#16a34a"; }
                    case "At Risk"  -> { bg = "#fef9c3"; fg = "#a16207"; }
                    default         -> { bg = "#fee2e2"; fg = "#dc2626"; }
                }
                badge.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg
                        + "; -fx-padding: 4 12 4 12; -fx-background-radius: 999; "
                        + "-fx-font-size: 12px; -fx-font-weight: 700;");
                setGraphic(badge); setText(null);
            }
        });

        attTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        attTable.setPlaceholder(new Label("No attendance records found."));
        attTable.setStyle("-fx-background-color: transparent;");
    }

    private void loadAttendance() {
        List<AttendanceRow> rows = new java.util.ArrayList<>();
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


    private <T> TableCell<T, String> styledCell(String color, boolean bold) {
        return new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(v);
                setStyle("-fx-font-size: 13px; -fx-font-weight: " + (bold ? "700" : "400")
                        + "; -fx-text-fill: " + color + ";");
            }
        };
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

    private String gradeBadgeColor(String g) {
        return switch (g) {
            case "A+" -> "#059669"; case "A"  -> "#16a34a";
            case "B+" -> "#2563eb"; case "B"  -> "#3b82f6";
            case "C"  -> "#d97706"; case "D"  -> "#ea580c";
            default   -> "#dc2626";
        };
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
