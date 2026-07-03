package com.hamroschool.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.hamroschool.model.dto.ReportCardEntry;
import com.hamroschool.model.dto.StudentMarkSummary;
import com.hamroschool.model.entity.AttendanceRecord;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class TeacherDashboardController {

    private final MarkService       markService       = MarkServiceImpl.getInstance();
    private final AttendanceService attendanceService = AttendanceServiceImpl.getInstance();
    private final TeacherService    teacherService    = TeacherServiceImpl.getInstance();

    private String          teacherUsername;
    private String          assignedSubject;
    private final LocalDate attendanceDate = LocalDate.now();

    // ── Cache for background-loaded data ──────────────────────────────────────
    private volatile List<String> cachedStudentList = List.of();
    private volatile boolean      dataLoaded        = false;

    /** Live map: studentUsername → current status (PRESENT/ABSENT/LATE) for today's session. */
    private final Map<String, String> pendingStatus = new HashMap<>();
    /** Live map: studentUsername → feedback note for today's session. */
    private final Map<String, String> pendingFeedback = new HashMap<>();
    /** Last saved status map for today's session (used to compute live percentages). */
    private final Map<String, String> savedStatusToday = new HashMap<>();
    /** Baseline historical totals from DB: studentUsername → [attendedClasses, totalClasses]. */
    private final Map<String, int[]> attendanceTotalsMap = new HashMap<>();
    /** Live map: studentUsername → attendance percentage including unsaved pending changes. */
    private final Map<String, Double> liveAttendancePctMap = new HashMap<>();

    // ── Top bar ──────────────────────────────────────────────────────────────
    @FXML private Label  pageTitle;
    @FXML private Label  pageSubtitle;
    @FXML private Label  userInitialsLabel;
    @FXML private Label  userNameLabel;
    @FXML private Button logoutButton;

    // ── Nav buttons ───────────────────────────────────────────────────────────
    @FXML private Button navAttendanceBtn;
    @FXML private Button navMarkSheetBtn;
    @FXML private Button navReportCardBtn;
    @FXML private Button navPerformanceBtn;

    // ── Section panes ─────────────────────────────────────────────────────────
    @FXML private VBox attendancePane;
    @FXML private VBox markSheetPane;
    @FXML private VBox reportCardPane;
    @FXML private VBox performancePane;

    // ── Attendance ────────────────────────────────────────────────────────────
    @FXML private Label  attTotalLabel;
    @FXML private Label  attPresentLabel;
    @FXML private Label  attAbsentLabel;
    @FXML private Label  attLateLabel;
    @FXML private Label  attDateLabel;
    @FXML private Label  attSubjectLabel;
    @FXML private Label  subjectTagLabel;
    @FXML private TextField attSearchField;
    @FXML private Button attMarkAllPresentBtn;
    @FXML private Button attSaveBtn;
    @FXML private TableView<String>            attTable;
    @FXML private TableColumn<String, String>  attColRoll;
    @FXML private TableColumn<String, String>  attColStudent;
    @FXML private TableColumn<String, String>  attColPct;
    @FXML private TableColumn<String, String>  attColStatus;
    @FXML private TableColumn<String, String>  attColFeedback;

    // ── Mark sheet ────────────────────────────────────────────────────────────
    @FXML private Label            msSheetTitle;
    @FXML private Label            msStatStudentsLabel;
    @FXML private Label            msStatAvgLabel;
    @FXML private Label            msStatPassLabel;
    @FXML private Label            msStatTopLabel;
    @FXML private VBox             msFormPane;
    @FXML private ComboBox<String> msStudentCombo;
    @FXML private TextField        msSubjectField;   // hidden, pre-filled
    @FXML private ComboBox<String> msExamTypeCombo;
    @FXML private TextField        msScoreField;
    @FXML private TextField        msFullMarksField;
    @FXML private TextField        msRemarksField;
    @FXML private Label            msStatusLabel;
    @FXML private TextField        msSearchField;
    @FXML private Label            msSummaryLabel;
    @FXML private Button           msAddMarkBtn;
    @FXML private Button           msSaveMarkBtn;
    @FXML private Button           msCancelFormBtn;
    @FXML private TableView<StudentMarkSummary>                 markSheetTable;
    @FXML private TableColumn<StudentMarkSummary, String>       msColRoll;
    @FXML private TableColumn<StudentMarkSummary, String>       msColStudent;
    @FXML private TableColumn<StudentMarkSummary, String>       msColMidterm;
    @FXML private TableColumn<StudentMarkSummary, String>       msColFinal;
    @FXML private TableColumn<StudentMarkSummary, String>       msColTotal;
    @FXML private TableColumn<StudentMarkSummary, String>       msColStatus;

    // ── Report card ───────────────────────────────────────────────────────────
    @FXML private Label                          rcCardTitle;
    @FXML private Label                          rcCardSubtitle;
    @FXML private Label                          rcStatStudentsLabel;
    @FXML private Label                          rcStatAvgGradeLabel;
    @FXML private Label                          rcStatPassLabel;
    @FXML private Label                          rcStatTopLabel;
    @FXML private TextField                      rcSearchField;
    @FXML private Label                          rcSummaryLabel;
    @FXML private TableView<ReportCardEntry>              rcTable;
    @FXML private TableColumn<ReportCardEntry, String>    rcColRoll;
    @FXML private TableColumn<ReportCardEntry, String>    rcColStudent;
    @FXML private TableColumn<ReportCardEntry, String>    rcColGrade;
    @FXML private TableColumn<ReportCardEntry, String>    rcColGpa;
    @FXML private TableColumn<ReportCardEntry, String>    rcColRank;
    @FXML private TableColumn<ReportCardEntry, String>    rcColStatus;

    // ── Performance ───────────────────────────────────────────────────────────
    @FXML private ComboBox<String>          pfSubjectCombo;
    @FXML private Button                    pfRefreshBtn;
    @FXML private Label                     pfAvgLabel;
    @FXML private Label                     pfHighLabel;
    @FXML private Label                     pfLowLabel;
    @FXML private Label                     pfPassLabel;
    @FXML private TableView<Mark>           performanceTable;
    @FXML private TableColumn<Mark, String> pfColRank;
    @FXML private TableColumn<Mark, String> pfColStudent;
    @FXML private TableColumn<Mark, String> pfColSubject;
    @FXML private TableColumn<Mark, String> pfColScore;
    @FXML private TableColumn<Mark, String> pfColPct;
    @FXML private TableColumn<Mark, String> pfColGrade;
    @FXML private TableColumn<Mark, String> pfColStatus;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        teacherUsername = SessionContext.getInstance().requireCurrentUser().getUsername();
        userInitialsLabel.setText(Utils.initials(teacherUsername));
        userNameLabel.setText(teacherUsername);

        // Resolve assigned subject
        Optional<String> sub = teacherService.getSubject(teacherUsername);
        assignedSubject = sub.orElse("");

        setupMarkTables();
        setupAttendanceTable();

        msExamTypeCombo.setItems(FXCollections.observableArrayList(
                "Terminal", "Mid-Term", "Final", "Unit Test", "Practical"));
        msExamTypeCombo.setValue("Terminal");
        msFullMarksField.setText("100");
        if (!assignedSubject.isBlank()) msSubjectField.setText(assignedSubject);

        msSearchField.textProperty().addListener((obs, o, n) -> loadMarkSheetTable(n));

        // Wire buttons from sub-FXML includes (no onAction in sub-files)
        attMarkAllPresentBtn.setOnAction(e -> handleMarkAllPresent());
        attSaveBtn.setOnAction(e -> handleSaveAttendance());
        msAddMarkBtn.setOnAction(e -> handleOpenAddMark());
        msSaveMarkBtn.setOnAction(e -> handleSaveMark());
        msCancelFormBtn.setOnAction(e -> handleClearMarkForm());
        pfRefreshBtn.setOnAction(e -> handleRefreshPerformance());

        showPane(attendancePane, navAttendanceBtn, "Attendance",
                "Mark and manage attendance for your assigned subject only");

        // Load student list once on background thread
        Thread loader = new Thread(() -> {
            try {
                cachedStudentList = markService.getAllStudentUsernames();
                dataLoaded = true;
                javafx.application.Platform.runLater(() -> {
                    msStudentCombo.setItems(FXCollections.observableArrayList(cachedStudentList));
                    refreshAttendance();
                });
            } catch (Exception ex) {
                System.err.println("[TeacherDashboard] Data load error: " + ex.getMessage());
            }
        }, "TeacherDataLoader");
        loader.setDaemon(true);
        loader.start();
    }

    // ── Nav ───────────────────────────────────────────────────────────────────

    @FXML private void handleNavAttendance() {
        showPane(attendancePane, navAttendanceBtn, "Attendance",
                "Mark and manage attendance for your assigned subject only");
        refreshAttendance();
    }

    @FXML private void handleNavMarkSheet() {
        showPane(markSheetPane, navMarkSheetBtn, "Mark Sheet",
                "Subject-assigned marksheet for the logged-in teacher. All marks, grades, and performance details shown here belong only to "
                + (assignedSubject.isBlank() ? "your subject" : assignedSubject) + ".");
        msFormPane.setVisible(false);
        msFormPane.setManaged(false);
        loadMarkSheetTable("");
        refreshMarkSheetStats();
    }

    @FXML private void handleNavReportCard() {
        String subject = assignedSubject.isBlank() ? "General" : assignedSubject;
        showPane(reportCardPane, navReportCardBtn, "Report Card",
                "Subject-assigned report card for the logged-in teacher. Only the report details for the admin-assigned subject are shown here.");
        rcCardTitle.setText(subject + " Report Card");
        rcCardSubtitle.setText("A concise subject-specific report card showing grade distribution, class standing, and performance summary for " + subject + " only.");
        loadReportCard("");
        refreshReportCardStats();
    }

    @FXML private void handleNavPerformance() {
        showPane(performancePane, navPerformanceBtn, "Performance Summary",
                "Class-wide performance analysis");
        refreshSubjectCombo();
        handleRefreshPerformance();
    }

    @FXML private void handleLogout() {
        SessionContext.getInstance().clear();
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/hello-view.fxml",
                "Hamro School", 920, 720);
    }

    // ── Attendance actions ────────────────────────────────────────────────────

    @FXML
    private void handleMarkAllPresent() {
        List<String> students = getFilteredStudents();
        for (String s : students) pendingStatus.put(s, "PRESENT");
        recalculateLiveAttendancePercentages();
        attTable.refresh();
        updateAttendanceSummary();
    }

    @FXML
    private void handleSaveAttendance() {
        if (!dataLoaded) return;
        String subject = assignedSubject.isBlank() ? "General" : assignedSubject;
        for (String s : cachedStudentList) {
            String status = pendingStatus.getOrDefault(s, "PRESENT");
            String feedback = pendingFeedback.getOrDefault(s, "");
            attendanceService.saveAttendance(s, teacherUsername, subject, attendanceDate, status, feedback);
        }
        refreshAttendance();
    }

    /** Called by the per-row Present/Late/Absent toggle buttons. */
    private void setStudentStatus(String studentUsername, String status) {
        pendingStatus.put(studentUsername, status);
        recalculateLiveAttendancePercentages();
        attTable.refresh();
        updateAttendanceSummary();
    }

    private void setStudentFeedback(String studentUsername, String feedback) {
        pendingFeedback.put(studentUsername, normalizeFeedback(feedback));
    }

    // ── Attendance helpers ────────────────────────────────────────────────────

    private void refreshAttendance() {
        if (!dataLoaded) return;
        
        String subject = assignedSubject.isBlank() ? "General" : assignedSubject;

        attDateLabel.setText("Today, " +
                attendanceDate.format(DateTimeFormatter.ofPattern("MMM d")));
        attSubjectLabel.setText("Assigned subject: " + (assignedSubject.isBlank() ? "—" : assignedSubject)
                + " · Read-only context set by admin");
        subjectTagLabel.setText(assignedSubject.isBlank() ? "No Subject" : assignedSubject);

        attendanceTotalsMap.clear();
        attendanceTotalsMap.putAll(attendanceService.getAttendanceTotals(teacherUsername, subject));

        Map<String, AttendanceRecord> todayRecords = new HashMap<>();
        for (AttendanceRecord record : attendanceService.getAttendanceForDate(teacherUsername, subject, attendanceDate)) {
            todayRecords.put(record.getStudentUsername(), record);
        }
        savedStatusToday.clear();
        todayRecords.forEach((student, record) -> savedStatusToday.put(student, record.getStatus()));

        // Seed pendingStatus from DB for today
        for (String s : cachedStudentList) {
            String savedStatus = Optional.ofNullable(todayRecords.get(s))
                    .map(AttendanceRecord::getStatus)
                    .orElse("PRESENT");
            String savedFeedback = Optional.ofNullable(todayRecords.get(s))
                    .map(AttendanceRecord::getFeedback)
                    .orElse("");

            pendingStatus.put(s, savedStatus);
            pendingFeedback.put(s, savedFeedback);
        }

        recalculateLiveAttendancePercentages();
        String query = attSearchField == null ? "" : attSearchField.getText();
        attTable.setItems(FXCollections.observableArrayList(getFilteredStudents(query)));
        attTotalLabel.setText(String.valueOf(cachedStudentList.size()));
        updateAttendanceSummary();
    }

    private void updateAttendanceSummary() {
        if (!dataLoaded) return;
        long present = cachedStudentList.stream().filter(s -> "PRESENT".equals(pendingStatus.getOrDefault(s,"PRESENT"))).count();
        long absent  = cachedStudentList.stream().filter(s -> "ABSENT".equals(pendingStatus.getOrDefault(s,"PRESENT"))).count();
        long late    = cachedStudentList.stream().filter(s -> "LATE".equals(pendingStatus.getOrDefault(s,"PRESENT"))).count();
        attPresentLabel.setText(String.valueOf(present));
        attAbsentLabel.setText(String.valueOf(absent));
        attLateLabel.setText(String.valueOf(late));
    }

    private List<String> getFilteredStudents() {
        return getFilteredStudents(attSearchField == null ? "" : attSearchField.getText());
    }

    private List<String> getFilteredStudents(String query) {
        if (!dataLoaded) return List.of();
        if (query == null || query.isBlank()) return cachedStudentList;
        String q = query.trim().toLowerCase(Locale.ROOT);
        return cachedStudentList.stream().filter(s -> s.toLowerCase(Locale.ROOT).contains(q)).toList();
    }

    private void recalculateLiveAttendancePercentages() {
        if (!dataLoaded) return;
        liveAttendancePctMap.clear();
        for (String student : cachedStudentList) {
            int[] baseline = attendanceTotalsMap.getOrDefault(student, new int[]{0, 0});
            int attended = baseline[0];
            int total = baseline[1];

            String currentStatus = pendingStatus.getOrDefault(student, "PRESENT");
            String savedStatus = savedStatusToday.get(student);

            if (savedStatus == null) {
                total += 1;
                if (isAttendedStatus(currentStatus)) attended += 1;
            } else {
                if (isAttendedStatus(savedStatus)) attended -= 1;
                if (isAttendedStatus(currentStatus)) attended += 1;
            }

            double pct = total > 0 ? Math.round(attended * 1000.0 / total) / 10.0 : 0.0;
            liveAttendancePctMap.put(student, pct);
        }
    }

    private static boolean isAttendedStatus(String status) {
        return "PRESENT".equals(status) || "LATE".equals(status);
    }

    private static String normalizeFeedback(String feedback) {
        if (feedback == null) return "";
        String compact = feedback.trim();
        return compact.length() <= 300 ? compact : compact.substring(0, 300);
    }

    private void setupAttendanceTable() {
        attColRoll.setCellValueFactory(c -> {
            int idx = attTable.getItems().indexOf(c.getValue());
            return new ReadOnlyStringWrapper(String.format("%02d", idx + 1));
        });
        attColRoll.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(v);
                setStyle("-fx-text-fill: #555555; -fx-font-size: 13px; -fx-alignment: center;");
            }
        });

        attColStudent.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue()));
        attColStudent.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String username, boolean empty) {
                super.updateItem(username, empty);
                if (empty || username == null) { setGraphic(null); return; }
                Label avatar = new Label(Utils.initials(username));
                avatar.setStyle("-fx-background-color: #e8e8e6; -fx-text-fill: #444444; " +
                    "-fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 999; " +
                    "-fx-min-width: 32; -fx-min-height: 32; -fx-pref-width: 32; -fx-pref-height: 32; " +
                    "-fx-alignment: center;");
                Label name = new Label(Utils.formatName(username));
                name.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #222222;");
                Label email = new Label(username + "@school.edu");
                email.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");
                VBox nameBox = new VBox(1, name, email);
                HBox box = new HBox(10, avatar, nameBox);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });

        // Attendance % column
        attColPct.setCellValueFactory(c -> {
            double pct = liveAttendancePctMap.getOrDefault(c.getValue(), 0.0);
            return new ReadOnlyStringWrapper(pct + "%");
        });
        attColPct.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                double pct = Double.parseDouble(v.replace("%",""));
                String color = pct >= 75 ? "#16a34a" : pct >= 50 ? "#d97706" : "#dc2626";
                setText(v);
                setStyle("-fx-text-fill: " + color + "; -fx-font-weight: 700; -fx-font-size: 13px; -fx-alignment: center;");
            }
        });

        // Status column — Present / Late / Absent toggle buttons
        attColStatus.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue()));
        attColStatus.setCellFactory(col -> new TableCell<>() {
            private final Button btnPresent = new Button("✓ Present");
            private final Button btnLate    = new Button("Late");
            private final Button btnAbsent  = new Button("✗ Absent");
            {
                String base = "-fx-cursor: hand; -fx-background-radius: 8; -fx-font-size: 12px; -fx-font-weight: 700; -fx-padding: 6 12 6 12;";
                btnPresent.setStyle(base + " -fx-background-color: #111111; -fx-text-fill: white;");
                btnLate.setStyle(base    + " -fx-background-color: transparent; -fx-border-color: #e6e4df; -fx-border-radius: 8; -fx-text-fill: #555555;");
                btnAbsent.setStyle(base  + " -fx-background-color: transparent; -fx-border-color: #e6e4df; -fx-border-radius: 8; -fx-text-fill: #555555;");
                btnPresent.setOnAction(e -> { if (getItem() != null) setStudentStatus(getItem(), "PRESENT"); });
                btnLate.setOnAction   (e -> { if (getItem() != null) setStudentStatus(getItem(), "LATE");    });
                btnAbsent.setOnAction (e -> { if (getItem() != null) setStudentStatus(getItem(), "ABSENT");  });
            }
            @Override protected void updateItem(String username, boolean empty) {
                super.updateItem(username, empty);
                if (empty || username == null) { setGraphic(null); return; }
                String status = pendingStatus.getOrDefault(username, "PRESENT");
                String base    = "-fx-cursor: hand; -fx-background-radius: 8; -fx-font-size: 12px; -fx-font-weight: 700; -fx-padding: 6 12 6 12;";
                String active  = base + " -fx-background-color: #f97316; -fx-text-fill: white;";
                String inactive= base + " -fx-background-color: transparent; -fx-border-color: #e6e4df; -fx-border-radius: 8; -fx-text-fill: #555555;";
                String absentActive = base + " -fx-background-color: #dc2626; -fx-text-fill: white;";
                btnPresent.setStyle("PRESENT".equals(status) ? active  : inactive);
                btnLate.setStyle   ("LATE".equals(status)    ? active  : inactive);
                btnAbsent.setStyle ("ABSENT".equals(status)  ? absentActive : inactive);
                HBox box = new HBox(6, btnPresent, btnLate, btnAbsent);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });

        attColFeedback.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue()));
        attColFeedback.setCellFactory(col -> new TableCell<>() {
            private final TextField feedbackField = new TextField();
            {
                feedbackField.setPromptText("Optional feedback");
                feedbackField.setStyle("-fx-font-size: 12px;");
                feedbackField.setOnAction(e -> commitEdit());
                feedbackField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                    if (!isFocused) commitEdit();
                });
            }

            private void commitEdit() {
                String username = getItem();
                if (username != null) {
                    setStudentFeedback(username, feedbackField.getText());
                }
            }

            @Override protected void updateItem(String username, boolean empty) {
                super.updateItem(username, empty);
                if (empty || username == null) {
                    setGraphic(null);
                    return;
                }
                feedbackField.setText(pendingFeedback.getOrDefault(username, ""));
                setGraphic(feedbackField);
            }
        });

        attTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        attTable.setPlaceholder(new Label("No students found."));
        attSearchField.textProperty().addListener((obs, o, n) -> {
            attTable.setItems(FXCollections.observableArrayList(getFilteredStudents(n)));
        });
    }

    // ── Mark sheet actions ────────────────────────────────────────────────────

    // ── Mark sheet actions ────────────────────────────────────────────────────

    @FXML
    private void handleOpenAddMark() {
        msFormPane.setVisible(true);
        msFormPane.setManaged(true);
    }

    @FXML
    private void handleSaveMark() {
        String student = msStudentCombo.getValue();
        String subject = assignedSubject.isBlank() ? msSubjectField.getText().trim() : assignedSubject;
        String exam    = msExamTypeCombo.getValue();
        if (student == null || subject.isBlank() || exam == null) {
            setMsStatus("Please fill Student and Exam Type.", false); return;
        }
        int score, fullMarks;
        try {
            score     = Integer.parseInt(msScoreField.getText().trim());
            fullMarks = Integer.parseInt(msFullMarksField.getText().trim());
        } catch (NumberFormatException e) {
            setMsStatus("Score and Full Marks must be whole numbers.", false); return;
        }
        if (score < 0 || score > fullMarks) {
            setMsStatus("Score must be between 0 and Full Marks.", false); return;
        }
        markService.saveMark(student, subject, teacherUsername, score, fullMarks,
                exam, msRemarksField.getText().trim());
        setMsStatus("Saved.", true);
        loadMarkSheetTable(msSearchField.getText());
        refreshMarkSheetStats();
    }

    @FXML
    private void handleClearMarkForm() {
        msStudentCombo.setValue(null);
        msExamTypeCombo.setValue("Mid-Term");
        msScoreField.clear();
        msFullMarksField.setText("50");
        msRemarksField.clear();
        msStatusLabel.setText("");
        msFormPane.setVisible(false);
        msFormPane.setManaged(false);
    }

    // ── Report card ───────────────────────────────────────────────────────────

    // ── Report card ───────────────────────────────────────────────────────────

    private void loadReportCard(String query) {
        String subject = assignedSubject.isBlank() ? "General" : assignedSubject;
        List<ReportCardEntry> all = markService.getReportCard(teacherUsername, subject);
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<ReportCardEntry> filtered = q.isBlank() ? all : all.stream()
                .filter(e -> e.getUsername().toLowerCase(Locale.ROOT).contains(q)).toList();
        rcTable.setItems(FXCollections.observableArrayList(filtered));
        rcSummaryLabel.setText("Showing " + filtered.size() + " students");
    }

    private void refreshReportCardStats() {
        if (!dataLoaded) return;
        String subject = assignedSubject.isBlank() ? "General" : assignedSubject;
        rcStatStudentsLabel.setText(String.valueOf(cachedStudentList.size()));
        double avg = markService.getAverageMarks(teacherUsername, subject);
        // average grade letter
        ReportCardEntry tmp = new ReportCardEntry(0, "", avg, 0);
        rcStatAvgGradeLabel.setText(avg > 0 ? tmp.getGrade() : "—");
        double pass = markService.getPassRate(teacherUsername, subject);
        rcStatPassLabel.setText(pass > 0 ? String.format("%.0f%%", pass) : "—");
        int top = markService.getTopScore(teacherUsername, subject);
        rcStatTopLabel.setText(top > 0 ? String.valueOf(top) : "—");
    }

    // ── Performance ───────────────────────────────────────────────────────────

    @FXML
    private void handleRefreshPerformance() {
        List<Mark> marks = markService.getMarksByTeacher(teacherUsername);
        String sub = pfSubjectCombo.getValue();
        if (sub != null && !sub.isBlank())
            marks = marks.stream().filter(m -> m.getSubjectName().equalsIgnoreCase(sub)).toList();
        if (marks.isEmpty()) {
            pfAvgLabel.setText("—"); pfHighLabel.setText("—");
            pfLowLabel.setText("—"); pfPassLabel.setText("—");
            performanceTable.setItems(FXCollections.emptyObservableList()); return;
        }
        double avg  = marks.stream().mapToDouble(Mark::getPercentage).average().orElse(0);
        double high = marks.stream().mapToDouble(Mark::getPercentage).max().orElse(0);
        double low  = marks.stream().mapToDouble(Mark::getPercentage).min().orElse(0);
        long   pass = marks.stream().filter(m -> m.getPercentage() >= 40).count();
        pfAvgLabel.setText(String.format("%.1f%%", avg));
        pfHighLabel.setText(String.format("%.1f%%", high));
        pfLowLabel.setText(String.format("%.1f%%", low));
        pfPassLabel.setText(pass + " / " + marks.size());
        List<Mark> ranked = marks.stream()
                .sorted((a, b) -> Double.compare(b.getPercentage(), a.getPercentage())).toList();
        performanceTable.setItems(FXCollections.observableArrayList(ranked));
    }

    // ── Table setup (marks) ───────────────────────────────────────────────────

    private void setupMarkTables() {
        // ── Marksheet summary table ──────────────────────────────────────────
        msColRoll.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(String.format("%02d", c.getValue().getRoll())));
        msColRoll.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v);
                setStyle("-fx-text-fill: #555555; -fx-font-size: 13px; -fx-alignment: center;");
            }
        });

        msColStudent.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getUsername()));
        msColStudent.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String username, boolean empty) {
                super.updateItem(username, empty);
                if (empty || username == null) { setGraphic(null); return; }
                Label avatar = new Label(Utils.initials(username));
                avatar.setStyle("-fx-background-color: #e8e8e6; -fx-text-fill: #444444; " +
                    "-fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 999; " +
                    "-fx-min-width: 32; -fx-min-height: 32; -fx-pref-width: 32; -fx-pref-height: 32; " +
                    "-fx-alignment: center;");
                Label name = new Label(Utils.formatName(username));
                name.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #222222;");
                HBox box = new HBox(10, avatar, name);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });

        msColMidterm.setCellValueFactory(c -> {
            int v = c.getValue().getMidterm();
            return new ReadOnlyStringWrapper(v < 0 ? "—" : String.valueOf(v));
        });
        msColMidterm.setCellFactory(col -> scoreCell("#f97316"));

        msColFinal.setCellValueFactory(c -> {
            int v = c.getValue().getFinalMark();
            return new ReadOnlyStringWrapper(v < 0 ? "—" : String.valueOf(v));
        });
        msColFinal.setCellFactory(col -> scoreCell("#f97316"));

        msColTotal.setCellValueFactory(c -> {
            StudentMarkSummary s = c.getValue();
            boolean hasAny = s.getMidterm() >= 0 || s.getFinalMark() >= 0;
            return new ReadOnlyStringWrapper(hasAny ? String.valueOf(s.getTotal()) : "—");
        });
        msColTotal.setCellFactory(col -> scoreCell("#f97316"));

        msColStatus.setCellValueFactory(c -> {
            StudentMarkSummary s = c.getValue();
            boolean hasAny = s.getMidterm() >= 0 || s.getFinalMark() >= 0;
            return new ReadOnlyStringWrapper(hasAny ? s.getStatus() : "—");
        });
        msColStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null || "—".equals(status)) {
                    setGraphic(null); setText(null); return;
                }
                Label badge = new Label(status);
                String bg, fg;
                switch (status) {
                    case "Pass"          -> { bg = "#f4f4f5"; fg = "#111111"; }
                    case "Average"       -> { bg = "#fef9c3"; fg = "#a16207"; }
                    default              -> { bg = "#fee2e2"; fg = "#dc2626"; } // Needs Support
                }
                badge.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; " +
                    "-fx-padding: 4 12 4 12; -fx-background-radius: 999; " +
                    "-fx-font-size: 12px; -fx-font-weight: 700;");
                setGraphic(badge); setText(null);
            }
        });

        markSheetTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        markSheetTable.setPlaceholder(new Label("No marks recorded yet."));
        markSheetTable.setStyle("-fx-background-color: transparent;");

        msSearchField.textProperty().addListener((obs, o, n) -> loadMarkSheetTable(n));

        rcColRoll.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(String.format("%02d", c.getValue().getRoll())));
        rcColRoll.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v);
                setStyle("-fx-text-fill: #555555; -fx-font-size: 13px; -fx-alignment: center;");
            }
        });

        rcColStudent.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getUsername()));
        rcColStudent.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String username, boolean empty) {
                super.updateItem(username, empty);
                if (empty || username == null) { setGraphic(null); return; }
                Label avatar = new Label(Utils.initials(username));
                avatar.setStyle("-fx-background-color: #e8e8e6; -fx-text-fill: #444444; " +
                    "-fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 999; " +
                    "-fx-min-width: 32; -fx-min-height: 32; -fx-pref-width: 32; -fx-pref-height: 32; " +
                    "-fx-alignment: center;");
                Label name = new Label(Utils.formatName(username));
                name.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #222222;");
                HBox box = new HBox(10, avatar, name);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });

        rcColGrade.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getGrade()));
        rcColGrade.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String g, boolean empty) {
                super.updateItem(g, empty);
                if (empty || g == null) { setText(null); return; }
                setText(g);
                setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #f97316; -fx-alignment: center;");
            }
        });

        rcColGpa.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(String.format("%.1f", c.getValue().getGpa())));
        rcColGpa.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v);
                setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #222222; -fx-alignment: center;");
            }
        });

        rcColRank.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.valueOf(c.getValue().getRank())));
        rcColRank.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v);
                setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #222222; -fx-alignment: center;");
            }
        });

        rcColStatus.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getStatus()));
        rcColStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(status);
                String bg, fg;
                switch (status) {
                    case "Excellent"     -> { bg = "#dcfce7"; fg = "#16a34a"; }
                    case "Good"          -> { bg = "#dbeafe"; fg = "#2563eb"; }
                    case "Average"       -> { bg = "#fef9c3"; fg = "#a16207"; }
                    default              -> { bg = "#fee2e2"; fg = "#dc2626"; }
                }
                badge.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; " +
                    "-fx-padding: 4 12 4 12; -fx-background-radius: 999; " +
                    "-fx-font-size: 12px; -fx-font-weight: 700;");
                setGraphic(badge); setText(null);
            }
        });

        rcTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        rcTable.setPlaceholder(new Label("No marks found."));
        rcSearchField.textProperty().addListener((obs, o, n) -> loadReportCard(n));

        pfColRank.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                String.valueOf(performanceTable.getItems().indexOf(c.getValue()) + 1)));
        pfColStudent.setCellValueFactory(c -> new ReadOnlyStringWrapper(Utils.formatName(c.getValue().getStudentUsername())));
        pfColSubject.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getSubjectName()));
        pfColScore.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getScore() + " / " + c.getValue().getFullMarks()));
        pfColPct.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getPercentage() + "%"));
        pfColGrade.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getGrade()));
        pfColStatus.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getPercentage() >= 40 ? "Pass" : "Fail"));
        pfColStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(s);
                boolean pass = "Pass".equals(s);
                badge.setStyle("-fx-background-color: " + (pass ? "#dcfce7" : "#fee2e2") + "; " +
                        "-fx-text-fill: " + (pass ? "#16a34a" : "#dc2626") + "; " +
                        "-fx-padding: 3 10 3 10; -fx-background-radius: 999; " +
                        "-fx-font-size: 11px; -fx-font-weight: 800;");
                setGraphic(badge); setText(null);
            }
        });
        setupGradeBadgeColumn(pfColGrade);
        performanceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        performanceTable.setPlaceholder(new Label("No performance data available."));
    }

    private void loadMarkSheetTable(String query) {
        String subject = assignedSubject.isBlank() ? "General" : assignedSubject;
        List<StudentMarkSummary> all = markService.getMarksheet(teacherUsername, subject);
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<StudentMarkSummary> filtered = q.isBlank() ? all : all.stream()
                .filter(s -> s.getUsername().toLowerCase(Locale.ROOT).contains(q)).toList();
        markSheetTable.setItems(FXCollections.observableArrayList(filtered));
        msSummaryLabel.setText("Showing " + filtered.size() + " students");
    }

    private void refreshMarkSheetStats() {
        if (!dataLoaded) return;
        String subject = assignedSubject.isBlank() ? "General" : assignedSubject;
        msSheetTitle.setText((assignedSubject.isBlank() ? "General" : assignedSubject) + " Marksheet");
        msStatStudentsLabel.setText(String.valueOf(cachedStudentList.size()));
        double avg = markService.getAverageMarks(teacherUsername, subject);
        msStatAvgLabel.setText(avg > 0 ? String.format("%.0f%%", avg) : "—");
        double pass = markService.getPassRate(teacherUsername, subject);
        msStatPassLabel.setText(pass > 0 ? String.format("%.0f%%", pass) : "—");
        int top = markService.getTopScore(teacherUsername, subject);
        msStatTopLabel.setText(top > 0 ? String.valueOf(top) : "—");
    }

    private TableCell<StudentMarkSummary, String> scoreCell(String activeColor) {
        return new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(v);
                boolean isDash = "—".equals(v);
                setStyle("-fx-font-size: 13px; -fx-font-weight: " + (isDash ? "400" : "700") +
                         "; -fx-text-fill: " + (isDash ? "#aaaaaa" : activeColor) +
                         "; -fx-alignment: center;");
            }
        };
    }

    private void refreshSubjectCombo() {
        List<String> subjects = markService.getSubjectsByTeacher(teacherUsername);
        ObservableList<String> items = FXCollections.observableArrayList();
        items.add("");
        items.addAll(subjects);
        pfSubjectCombo.setItems(items);
    }


    private static final String ACTIVE_STYLE =
            "-fx-background-color: #111111; -fx-background-radius: 8; -fx-text-fill: white; " +
            "-fx-font-size: 13px; -fx-font-weight: 700; -fx-padding: 10 14 10 14; -fx-cursor: hand;";
    private static final String INACTIVE_STYLE =
            "-fx-background-color: transparent; -fx-background-radius: 8; -fx-text-fill: #272727; " +
            "-fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 10 14 10 14; -fx-cursor: hand;";

    private void showPane(VBox target, Button activeBtn, String title, String subtitle) {
        for (VBox pane : List.of(attendancePane, markSheetPane, reportCardPane, performancePane)) {
            pane.setVisible(pane == target);
            pane.setManaged(pane == target);
        }
        for (Button btn : List.of(navAttendanceBtn, navMarkSheetBtn, navReportCardBtn, navPerformanceBtn)) {
            btn.setStyle(btn == activeBtn ? ACTIVE_STYLE : INACTIVE_STYLE);
        }
        pageTitle.setText(title);
        pageSubtitle.setText(subtitle);
    }

    private void setMsStatus(String msg, boolean ok) {
        msStatusLabel.setText(msg);
        msStatusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: "
                + (ok ? "#16a34a" : "#dc2626") + ";");
    }

    private <T> void setupGradeBadgeColumn(TableColumn<T, String> col) {
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String grade, boolean empty) {
                super.updateItem(grade, empty);
                if (empty || grade == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(grade);
                badge.setStyle("-fx-background-color: " + gradeBadgeColor(grade)
                        + "; -fx-text-fill: white; -fx-padding: 3 10 3 10; " +
                        "-fx-background-radius: 999; -fx-font-size: 11px; -fx-font-weight: 800;");
                setGraphic(badge); setText(null);
            }
        });
    }

    private String gradeBadgeColor(String g) {
        return switch (g) {
            case "A+" -> "#059669"; case "A"  -> "#16a34a";
            case "B+" -> "#2563eb"; case "B"  -> "#3b82f6";
            case "C"  -> "#d97706"; case "D"  -> "#ea580c";
            default   -> "#dc2626";
        };
    }
}
