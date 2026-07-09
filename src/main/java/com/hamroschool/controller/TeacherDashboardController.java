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

import javafx.animation.PauseTransition;
import javafx.application.Platform;
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
import javafx.util.Duration;

public class TeacherDashboardController {

    private final MarkService       markService       = MarkServiceImpl.getInstance();
    private final AttendanceService attendanceService = AttendanceServiceImpl.getInstance();
    private final TeacherService    teacherService    = TeacherServiceImpl.getInstance();

    private String          teacherUsername;
    private String          assignedSubject;
    private final LocalDate attendanceDate = LocalDate.now();

    // ── In-memory state ───────────────────────────────────────────────────────
    private volatile List<String> cachedStudentList   = List.of();
    private volatile boolean      dataLoaded          = false;

    // Attendance working state
    private final Map<String, String> pendingStatus      = new HashMap<>();
    private final Map<String, String> pendingFeedback    = new HashMap<>();
    private final Map<String, String> savedStatusToday   = new HashMap<>();
    private final Map<String, int[]>  attendanceTotalsMap = new HashMap<>();
    private final Map<String, Double> liveAttendancePctMap = new HashMap<>();

    // Mark / report card caches — avoid repeated DB round-trips per navigation
    private List<StudentMarkSummary> cachedMarkSheet     = List.of();
    private List<ReportCardEntry>    cachedReportCard    = List.of();
    private List<Mark>               cachedTeacherMarks  = List.of();
    private List<String>             cachedSubjectList   = List.of();
    private boolean markSheetDirty   = true;
    private boolean reportCardDirty  = true;
    private boolean performanceDirty = true;
    private boolean subjectsDirty    = true;


    @FXML private Label  pageTitle;
    @FXML private Label  pageSubtitle;
    @FXML private Label  userInitialsLabel;
    @FXML private Label  userNameLabel;
    @FXML private Button logoutButton;

    @FXML private Label detailTeacherId;
    @FXML private Label detailTeacherName;
    @FXML private Label detailTeacherGender;
    @FXML private Label detailTeacherPhone;
    @FXML private Label detailTeacherEmail;
    @FXML private Label detailTeacherSubject;
    @FXML private Label detailTeacherQualification;
    @FXML private Label detailTeacherEmployment;

    @FXML private Button navDashboardBtn;
    @FXML private Button navAttendanceBtn;
    @FXML private Button navMarkSheetBtn;
    @FXML private Button navReportCardBtn;
    @FXML private Button navPerformanceBtn;

    @FXML private VBox dashboardPane;
    @FXML private VBox attendancePane;
    @FXML private VBox markSheetPane;
    @FXML private VBox reportCardPane;
    @FXML private VBox performancePane;

    @FXML private Label     attTotalLabel;
    @FXML private Label     attPresentLabel;
    @FXML private Label     attAbsentLabel;
    @FXML private Label     attLateLabel;
    @FXML private Label     attDateLabel;
    @FXML private Label     attSubjectLabel;
    @FXML private Label     subjectTagLabel;
    @FXML private Label     attStatusLabel;
    @FXML private TextField attSearchField;
    @FXML private Button    attMarkAllPresentBtn;
    @FXML private Button    attSaveBtn;
    @FXML private TableView<String>           attTable;
    @FXML private TableColumn<String, String> attColRoll;
    @FXML private TableColumn<String, String> attColStudent;
    @FXML private TableColumn<String, String> attColPct;
    @FXML private TableColumn<String, String> attColStatus;
    @FXML private TableColumn<String, String> attColFeedback;

    @FXML private Label            msSheetTitle;
    @FXML private Label            msStatStudentsLabel;
    @FXML private Label            msStatAvgLabel;
    @FXML private Label            msStatPassLabel;
    @FXML private Label            msStatTopLabel;
    @FXML private VBox             msFormPane;
    @FXML private ComboBox<String> msStudentCombo;
    @FXML private TextField        msSubjectField;
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
    @FXML private TableView<StudentMarkSummary>           markSheetTable;
    @FXML private TableColumn<StudentMarkSummary, String> msColRoll;
    @FXML private TableColumn<StudentMarkSummary, String> msColStudent;
    @FXML private TableColumn<StudentMarkSummary, String> msColMidterm;
    @FXML private TableColumn<StudentMarkSummary, String> msColFinal;
    @FXML private TableColumn<StudentMarkSummary, String> msColTotal;
    @FXML private TableColumn<StudentMarkSummary, String> msColStatus;

    @FXML private Label                          rcCardTitle;
    @FXML private Label                          rcCardSubtitle;
    @FXML private Label                          rcStatStudentsLabel;
    @FXML private Label                          rcStatAvgGradeLabel;
    @FXML private Label                          rcStatPassLabel;
    @FXML private Label                          rcStatTopLabel;
    @FXML private TextField                      rcSearchField;
    @FXML private Label                          rcSummaryLabel;
    @FXML private TableView<ReportCardEntry>           rcTable;
    @FXML private TableColumn<ReportCardEntry, String> rcColRoll;
    @FXML private TableColumn<ReportCardEntry, String> rcColStudent;
    @FXML private TableColumn<ReportCardEntry, String> rcColGrade;
    @FXML private TableColumn<ReportCardEntry, String> rcColGpa;
    @FXML private TableColumn<ReportCardEntry, String> rcColRank;
    @FXML private TableColumn<ReportCardEntry, String> rcColStatus;

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



    @FXML
    public void initialize() {
        teacherUsername = SessionContext.getInstance().requireCurrentUser().getUsername();
        userInitialsLabel.setText(Utils.initials(teacherUsername));
        userNameLabel.setText(teacherUsername);

        Optional<String> sub = teacherService.getSubject(teacherUsername);
        assignedSubject = sub.orElse("");

        loadTeacherDetails();
        setupMarkTables();
        setupAttendanceTable();

        msExamTypeCombo.setItems(FXCollections.observableArrayList(
                "Terminal", "Mid-Term", "Final", "Unit Test", "Practical"));
        msExamTypeCombo.setValue("Terminal");
        msFullMarksField.setText("100");
        if (!assignedSubject.isBlank()) msSubjectField.setText(assignedSubject);

        attMarkAllPresentBtn.setOnAction(e -> handleMarkAllPresent());
        attSaveBtn.setOnAction(e -> handleSaveAttendance());
        msAddMarkBtn.setOnAction(e -> handleOpenAddMark());
        msSaveMarkBtn.setOnAction(e -> handleSaveMark());
        msCancelFormBtn.setOnAction(e -> handleClearMarkForm());
        pfRefreshBtn.setOnAction(e -> handleRefreshPerformance());

        showPane(dashboardPane, navDashboardBtn, "Dashboard",
                "Overview of your teaching profile and account information");

        Thread loader = new Thread(() -> {
            try {
                List<String> students = markService.getAllStudentUsernames();
                javafx.application.Platform.runLater(() -> {
                    cachedStudentList = students;
                    dataLoaded = true;
                    msStudentCombo.setItems(FXCollections.observableArrayList(students));
                    refreshAttendance();
                });
            } catch (Exception ex) {
                System.err.println("[TeacherDashboard] Data load error: " + ex.getMessage());
            }
        }, "TeacherDataLoader");
        loader.setDaemon(true);
        loader.start();
    }

    private void loadTeacherDetails() {
        var user = SessionContext.getInstance().requireCurrentUser();
        detailTeacherId.setText(user.getUserId().isEmpty() ? "TCH-0012" : user.getUserId());
        detailTeacherName.setText(user.getFullName().isEmpty() ? "—" : user.getFullName());
        detailTeacherGender.setText(user.getGender().isEmpty() ? "—" : user.getGender());
        detailTeacherPhone.setText(user.getPhone().isEmpty() ? "—" : user.getPhone());
        detailTeacherEmail.setText(user.getEmail().isEmpty() ? "—" : user.getEmail());
        detailTeacherSubject.setText(assignedSubject.isEmpty() ? "—" : assignedSubject);
        detailTeacherQualification.setText(user.getQualification().isEmpty() ? "—" : user.getQualification());
        detailTeacherEmployment.setText(user.getEmploymentStatus().isEmpty() ? "Full-time" : user.getEmploymentStatus());
    }


    @FXML private void handleNavDashboard() {
        showPane(dashboardPane, navDashboardBtn, "Dashboard",
                "Overview of your teaching profile and account information");
    }

    @FXML private void handleNavAttendance() {
        showPane(attendancePane, navAttendanceBtn, "Attendance",
                "Mark and manage attendance for your assigned subject only");
        refreshAttendance();
    }

    @FXML private void handleNavMarkSheet() {
        showPane(markSheetPane, navMarkSheetBtn, "Mark Sheet",
                "Marksheet for " + (assignedSubject.isBlank() ? "your subject" : assignedSubject) + ".");
        msFormPane.setVisible(false);
        msFormPane.setManaged(false);
        if (markSheetDirty) {
            refreshMarkSheetCache();
            markSheetDirty = false;
        }
        applyMarkSheetFilter(msSearchField.getText());
        refreshMarkSheetStats();
    }

    @FXML private void handleNavReportCard() {
        String subject = assignedSubject.isBlank() ? "General" : assignedSubject;
        showPane(reportCardPane, navReportCardBtn, "Report Card",
                "Subject-assigned report card for " + subject + ".");
        rcCardTitle.setText(subject + " Report Card");
        rcCardSubtitle.setText("Grade distribution, class standing, and performance summary for " + subject + ".");
        if (reportCardDirty) {
            refreshReportCardCache();
            reportCardDirty = false;
        }
        applyReportCardFilter(rcSearchField.getText());
        refreshReportCardStats();
    }

    @FXML private void handleNavPerformance() {
        showPane(performancePane, navPerformanceBtn, "Performance Summary",
                "Class-wide performance analysis");
        refreshSubjectCombo();
        if (performanceDirty) {
            refreshPerformanceCache();
            performanceDirty = false;
        }
        applyPerformanceFilter();
    }

    @FXML private void handleLogout() {
        SessionContext.getInstance().clear();
        SceneSwitcher.clearCache(); // drop cached scenes so next session gets fresh views
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/hello-view.fxml",
                "Hamro School", SceneSwitcher.LOGIN_WIDTH, SceneSwitcher.LOGIN_HEIGHT);
    }



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
        if (!dataLoaded) {
            setAttStatus("Data not loaded yet. Please wait.", false);
            return;
        }
        String subject = assignedSubject.isBlank() ? "General" : assignedSubject;
        List<String> students = List.copyOf(cachedStudentList);
        Map<String, String> statusSnapshot  = Map.copyOf(pendingStatus);
        Map<String, String> feedbackSnapshot = Map.copyOf(pendingFeedback);

        attSaveBtn.setDisable(true);
        setAttStatus("Saving…", true);

        Thread saver = new Thread(() -> {
            for (String s : students) {
                attendanceService.saveAttendance(s, teacherUsername, subject, attendanceDate,
                        statusSnapshot.getOrDefault(s, "PRESENT"),
                        feedbackSnapshot.getOrDefault(s, ""));
            }
            Platform.runLater(() -> {
                attSaveBtn.setDisable(false);
                setAttStatus("✓ Attendance saved for " + students.size() + " student(s)", true);
                markSheetDirty  = true;
                reportCardDirty = true;
                performanceDirty = true;
                refreshAttendance();
                PauseTransition pause = new PauseTransition(Duration.seconds(3));
                pause.setOnFinished(e -> attStatusLabel.setText(""));
                pause.play();
            });
        }, "AttendanceSaver");
        saver.setDaemon(true);
        saver.start();
    }

    private void setStudentStatus(String studentUsername, String status) {
        pendingStatus.put(studentUsername, status);
        recalculateSingleStudentPct(studentUsername);
        attTable.refresh();
        updateAttendanceSummary();
    }

    private void setStudentFeedback(String studentUsername, String feedback) {
        pendingFeedback.put(studentUsername, normalizeFeedback(feedback));
    }

    private void refreshAttendance() {
        if (!dataLoaded) return;
        String subject = assignedSubject.isBlank() ? "General" : assignedSubject;

        attDateLabel.setText("Today, " + attendanceDate.format(DateTimeFormatter.ofPattern("MMM d")));
        attSubjectLabel.setText("Assigned subject: " + (assignedSubject.isBlank() ? "—" : assignedSubject)
                + " · Read-only context set by admin");
        subjectTagLabel.setText(assignedSubject.isBlank() ? "No Subject" : assignedSubject);

        Thread loader = new Thread(() -> {
            Map<String, int[]>       totals      = attendanceService.getAttendanceTotals(teacherUsername, subject);
            List<AttendanceRecord>   todayList   = attendanceService.getAttendanceForDate(teacherUsername, subject, attendanceDate);

            Platform.runLater(() -> {
                attendanceTotalsMap.clear();
                attendanceTotalsMap.putAll(totals);

                Map<String, AttendanceRecord> todayRecords = new HashMap<>();
                for (AttendanceRecord record : todayList) {
                    todayRecords.put(record.getStudentUsername(), record);
                }
                savedStatusToday.clear();
                todayRecords.forEach((student, record) -> savedStatusToday.put(student, record.getStatus()));

                for (String s : cachedStudentList) {
                    pendingStatus.put(s, Optional.ofNullable(todayRecords.get(s))
                            .map(AttendanceRecord::getStatus).orElse("PRESENT"));
                    pendingFeedback.put(s, Optional.ofNullable(todayRecords.get(s))
                            .map(AttendanceRecord::getFeedback).orElse(""));
                }

                recalculateLiveAttendancePercentages();
                String query = attSearchField == null ? "" : attSearchField.getText();
                attTable.setItems(FXCollections.observableArrayList(getFilteredStudents(query)));
                attTotalLabel.setText(String.valueOf(cachedStudentList.size()));
                updateAttendanceSummary();
            });
        }, "AttendanceLoader");
        loader.setDaemon(true);
        loader.start();
    }

    private void updateAttendanceSummary() {
        if (!dataLoaded) return;
        long present = 0, absent = 0, late = 0;
        for (String s : cachedStudentList) {
            switch (pendingStatus.getOrDefault(s, "PRESENT")) {
                case "PRESENT" -> present++;
                case "ABSENT"  -> absent++;
                case "LATE"    -> late++;
            }
        }
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
            liveAttendancePctMap.put(student, calcPct(student));
        }
    }

    private void recalculateSingleStudentPct(String student) {
        liveAttendancePctMap.put(student, calcPct(student));
    }

    private double calcPct(String student) {
        int[] baseline = attendanceTotalsMap.getOrDefault(student, new int[]{0, 0});
        int attended = baseline[0];
        int total    = baseline[1];
        String currentStatus = pendingStatus.getOrDefault(student, "PRESENT");
        String savedStatus   = savedStatusToday.get(student);
        if (savedStatus == null) {
            total += 1;
            if (isAttendedStatus(currentStatus)) attended += 1;
        } else {
            if (isAttendedStatus(savedStatus))   attended -= 1;
            if (isAttendedStatus(currentStatus)) attended += 1;
        }
        return total > 0 ? Math.round(attended * 1000.0 / total) / 10.0 : 0.0;
    }

    private static boolean isAttendedStatus(String status) {
        return "PRESENT".equals(status) || "LATE".equals(status);
    }

    private static String normalizeFeedback(String feedback) {
        if (feedback == null) return "";
        String s = feedback.trim();
        return s.length() <= 300 ? s : s.substring(0, 300);
    }



    private void setupAttendanceTable() {
        attColRoll.setCellValueFactory(c -> new ReadOnlyStringWrapper(""));
        attColRoll.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setText(null); return; }
                setText(String.format("%02d", getIndex() + 1));
                setStyle("-fx-text-fill: #555555; -fx-font-size: 13px; -fx-alignment: center;");
            }
        });

        attColStudent.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue()));
        attColStudent.setCellFactory(col -> new TableCell<>() {
            private final Label avatar   = new Label();
            private final Label name     = new Label();
            private final Label email    = new Label();
            private final VBox  nameBox  = new VBox(1, name, email);
            private final HBox  box      = new HBox(10, avatar, nameBox);
            {
                avatar.setStyle("-fx-background-color: #e8e8e6; -fx-text-fill: #444444; " +
                    "-fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 999; " +
                    "-fx-min-width: 32; -fx-min-height: 32; -fx-pref-width: 32; -fx-pref-height: 32; " +
                    "-fx-alignment: center;");
                name.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #222222;");
                email.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");
                box.setAlignment(Pos.CENTER_LEFT);
            }
            @Override protected void updateItem(String username, boolean empty) {
                super.updateItem(username, empty);
                if (empty || username == null) { setGraphic(null); return; }
                avatar.setText(Utils.initials(username));
                name.setText(Utils.formatName(username));
                email.setText(username + "@school.edu");
                setGraphic(box);
            }
        });

        attColPct.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(liveAttendancePctMap.getOrDefault(c.getValue(), 0.0) + "%"));
        attColPct.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                double pct = Double.parseDouble(v.replace("%", ""));
                String color = pct >= 75 ? "#16a34a" : pct >= 50 ? "#d97706" : "#dc2626";
                setText(v);
                setStyle("-fx-text-fill: " + color + "; -fx-font-weight: 700; -fx-font-size: 13px; -fx-alignment: center;");
            }
        });

        attColStatus.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue()));
        attColStatus.setCellFactory(col -> new TableCell<>() {
            private final Button btnPresent = new Button("✓ Present");
            private final Button btnLate    = new Button("Late");
            private final Button btnAbsent  = new Button("✗ Absent");
            private final HBox   box        = new HBox(6, btnPresent, btnLate, btnAbsent);
            private static final String BASE     = "-fx-cursor: hand; -fx-background-radius: 8; -fx-font-size: 12px; -fx-font-weight: 700; -fx-padding: 6 12 6 12;";
            private static final String INACTIVE = BASE + " -fx-background-color: transparent; -fx-border-color: #e6e4df; -fx-border-radius: 8; -fx-text-fill: #555555;";
            private static final String ACTIVE_P = BASE + " -fx-background-color: #f97316; -fx-text-fill: white;";
            private static final String ACTIVE_A = BASE + " -fx-background-color: #dc2626; -fx-text-fill: white;";
            {
                btnPresent.setOnAction(e -> { if (getItem() != null) setStudentStatus(getItem(), "PRESENT"); });
                btnLate.setOnAction   (e -> { if (getItem() != null) setStudentStatus(getItem(), "LATE");    });
                btnAbsent.setOnAction (e -> { if (getItem() != null) setStudentStatus(getItem(), "ABSENT");  });
                box.setAlignment(Pos.CENTER_LEFT);
            }
            @Override protected void updateItem(String username, boolean empty) {
                super.updateItem(username, empty);
                if (empty || username == null) { setGraphic(null); return; }
                String status = pendingStatus.getOrDefault(username, "PRESENT");
                btnPresent.setStyle("PRESENT".equals(status) ? ACTIVE_P : INACTIVE);
                btnLate.setStyle   ("LATE".equals(status)    ? ACTIVE_P : INACTIVE);
                btnAbsent.setStyle ("ABSENT".equals(status)  ? ACTIVE_A : INACTIVE);
                setGraphic(box);
            }
        });

        attColFeedback.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue()));
        attColFeedback.setCellFactory(col -> new TableCell<>() {
            private final TextField feedbackField = new TextField();
            {
                feedbackField.setPromptText("Optional feedback");
                feedbackField.setStyle("-fx-font-size: 12px;");
                feedbackField.setOnAction(e -> commit());
                feedbackField.focusedProperty().addListener((obs, was, now) -> { if (!now) commit(); });
            }
            private void commit() {
                String username = getItem();
                if (username != null) setStudentFeedback(username, feedbackField.getText());
            }
            @Override protected void updateItem(String username, boolean empty) {
                super.updateItem(username, empty);
                if (empty || username == null) { setGraphic(null); return; }
                feedbackField.setText(pendingFeedback.getOrDefault(username, ""));
                setGraphic(feedbackField);
            }
        });

        attTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        attTable.setPlaceholder(new Label("No students found."));
        attSearchField.textProperty().addListener((obs, o, n) ->
                attTable.setItems(FXCollections.observableArrayList(getFilteredStudents(n))));
    }



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
        markSheetDirty  = true;
        reportCardDirty = true;
        performanceDirty = true;
        subjectsDirty    = true;
        setMsStatus("Saved.", true);
        refreshMarkSheetCache();
        applyMarkSheetFilter(msSearchField.getText());
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

    private void refreshMarkSheetCache() {
        String subject = assignedSubject.isBlank() ? "General" : assignedSubject;
        cachedMarkSheet = markService.getMarksheet(teacherUsername, subject);
        markSheetDirty  = false;
    }

    private void applyMarkSheetFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<StudentMarkSummary> filtered = q.isBlank() ? cachedMarkSheet
                : cachedMarkSheet.stream()
                    .filter(s -> s.getUsername().toLowerCase(Locale.ROOT).contains(q)).toList();
        markSheetTable.setItems(FXCollections.observableArrayList(filtered));
        msSummaryLabel.setText("Showing " + filtered.size() + " students");
    }

    private void refreshMarkSheetStats() {
        if (!dataLoaded) return;
        String subject = assignedSubject.isBlank() ? "General" : assignedSubject;
        msSheetTitle.setText((subject.equals("General") ? "General" : subject) + " Marksheet");
        msStatStudentsLabel.setText(String.valueOf(cachedStudentList.size()));
        List<StudentMarkSummary> withMarks = cachedMarkSheet.stream()
                .filter(s -> s.getMidterm() >= 0 || s.getFinalMark() >= 0).toList();
        if (withMarks.isEmpty()) {
            msStatAvgLabel.setText("—");
            msStatPassLabel.setText("—");
            msStatTopLabel.setText("—");
            return;
        }
        double avg = withMarks.stream().mapToDouble(StudentMarkSummary::getPercentage).average().orElse(0);
        msStatAvgLabel.setText(String.format("%.0f%%", avg));
        long passed = withMarks.stream().filter(s -> s.getPercentage() >= 40).count();
        double passRate = Math.round(passed * 1000.0 / withMarks.size()) / 10.0;
        msStatPassLabel.setText(String.format("%.0f%%", passRate));
        int top = withMarks.stream().mapToInt(StudentMarkSummary::getTotal).max().orElse(0);
        msStatTopLabel.setText(String.valueOf(top));
    }



    private void refreshReportCardCache() {
        String subject = assignedSubject.isBlank() ? "General" : assignedSubject;
        cachedReportCard = markService.getReportCard(teacherUsername, subject);
        reportCardDirty  = false;
    }

    private void applyReportCardFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<ReportCardEntry> filtered = q.isBlank() ? cachedReportCard
                : cachedReportCard.stream()
                    .filter(e -> e.getUsername().toLowerCase(Locale.ROOT).contains(q)).toList();
        rcTable.setItems(FXCollections.observableArrayList(filtered));
        rcSummaryLabel.setText("Showing " + filtered.size() + " students");
    }

    private void refreshReportCardStats() {
        if (!dataLoaded) return;
        rcStatStudentsLabel.setText(String.valueOf(cachedStudentList.size()));
        if (cachedReportCard.isEmpty()) {
            rcStatAvgGradeLabel.setText("—");
            rcStatPassLabel.setText("—");
            rcStatTopLabel.setText("—");
            return;
        }
        double avg = cachedReportCard.stream()
                .filter(e -> e.getPercentage() > 0)
                .mapToDouble(ReportCardEntry::getPercentage).average().orElse(0);
        rcStatAvgGradeLabel.setText(avg > 0 ? new ReportCardEntry(0, "", avg, 0).getGrade() : "—");
        long passed = cachedReportCard.stream().filter(e -> e.getPercentage() >= 40).count();
        double passRate = Math.round(passed * 1000.0 / cachedReportCard.size()) / 10.0;
        rcStatPassLabel.setText(passRate > 0 ? String.format("%.0f%%", passRate) : "—");
        int top = cachedReportCard.stream()
                .mapToInt(e -> (int) e.getPercentage()).max().orElse(0);
        rcStatTopLabel.setText(top > 0 ? top + "%" : "—");
    }


    private void refreshPerformanceCache() {
        cachedTeacherMarks  = markService.getMarksByTeacher(teacherUsername);
        performanceDirty = false;
    }

    @FXML
    private void handleRefreshPerformance() {
        refreshPerformanceCache();
        applyPerformanceFilter();
    }

    private void applyPerformanceFilter() {
        String sub = pfSubjectCombo.getValue();
        List<Mark> marks = (sub != null && !sub.isBlank())
                ? cachedTeacherMarks.stream()
                    .filter(m -> m.getSubjectName().equalsIgnoreCase(sub)).toList()
                : cachedTeacherMarks;

        if (marks.isEmpty()) {
            pfAvgLabel.setText("—"); pfHighLabel.setText("—");
            pfLowLabel.setText("—"); pfPassLabel.setText("—");
            performanceTable.setItems(FXCollections.emptyObservableList());
            return;
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

    private void refreshSubjectCombo() {
        if (!subjectsDirty) {
            return;
        }
        List<String> subjects = markService.getSubjectsByTeacher(teacherUsername);
        cachedSubjectList = subjects;
        subjectsDirty = false;

        ObservableList<String> items = FXCollections.observableArrayList();
        items.add("");
        items.addAll(subjects);
        pfSubjectCombo.setItems(items);
    }



    private void setupMarkTables() {
        msColRoll.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(String.format("%02d", c.getValue().getRoll())));
        msColRoll.setCellFactory(col -> plainCell("#555555", false, true));

        msColStudent.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getUsername()));
        msColStudent.setCellFactory(col -> avatarCell());

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
        msColStatus.setCellFactory(col -> statusBadgeCell(Map.of(
                "Pass",    new String[]{"#f4f4f5", "#111111"},
                "Average", new String[]{"#fef9c3", "#a16207"}),
                new String[]{"#fee2e2", "#dc2626"}));

        markSheetTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        markSheetTable.setPlaceholder(new Label("No marks recorded yet."));
        markSheetTable.setStyle("-fx-background-color: transparent;");
        msSearchField.textProperty().addListener((obs, o, n) -> applyMarkSheetFilter(n));
        rcColRoll.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(String.format("%02d", c.getValue().getRoll())));
        rcColRoll.setCellFactory(col -> plainCell("#555555", false, true));

        rcColStudent.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getUsername()));
        rcColStudent.setCellFactory(col -> avatarCell());

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
        rcColGpa.setCellFactory(col -> plainCell("#222222", true, true));

        rcColRank.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.valueOf(c.getValue().getRank())));
        rcColRank.setCellFactory(col -> plainCell("#222222", true, true));

        rcColStatus.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getStatus()));
        rcColStatus.setCellFactory(col -> statusBadgeCell(Map.of(
                "Excellent", new String[]{"#dcfce7", "#16a34a"},
                "Good",      new String[]{"#dbeafe", "#2563eb"},
                "Average",   new String[]{"#fef9c3", "#a16207"}),
                new String[]{"#fee2e2", "#dc2626"}));

        rcTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        rcTable.setPlaceholder(new Label("No marks found."));
        rcSearchField.textProperty().addListener((obs, o, n) -> applyReportCardFilter(n));

        pfColRank.setCellValueFactory(c -> new ReadOnlyStringWrapper(""));
        pfColRank.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty ? null : String.valueOf(getIndex() + 1));
                setStyle("-fx-text-fill: #555555; -fx-font-size: 13px; -fx-alignment: center;");
            }
        });

        pfColStudent.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(Utils.formatName(c.getValue().getStudentUsername())));
        pfColSubject.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getSubjectName()));
        pfColScore.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getScore() + " / " + c.getValue().getFullMarks()));
        pfColPct.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getPercentage() + "%"));
        pfColGrade.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getGrade()));
        setupGradeBadgeColumn(pfColGrade);

        pfColStatus.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getPercentage() >= 40 ? "Pass" : "Fail"));
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

        performanceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        performanceTable.setPlaceholder(new Label("No performance data available."));
    }



    private <T> TableCell<T, String> avatarCell() {
        return new TableCell<>() {
            private final Label avatar  = new Label();
            private final Label name    = new Label();
            private final HBox  box     = new HBox(10, avatar, name);
            {
                avatar.setStyle("-fx-background-color: #e8e8e6; -fx-text-fill: #444444; " +
                    "-fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 999; " +
                    "-fx-min-width: 32; -fx-min-height: 32; -fx-pref-width: 32; -fx-pref-height: 32; " +
                    "-fx-alignment: center;");
                name.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #222222;");
                box.setAlignment(Pos.CENTER_LEFT);
            }
            @Override protected void updateItem(String username, boolean empty) {
                super.updateItem(username, empty);
                if (empty || username == null) { setGraphic(null); return; }
                avatar.setText(Utils.initials(username));
                name.setText(Utils.formatName(username));
                setGraphic(box);
            }
        };
    }

    private <T> TableCell<T, String> plainCell(String color, boolean bold, boolean centered) {
        String style = "-fx-font-size: 13px; -fx-text-fill: " + color + "; "
                + "-fx-font-weight: " + (bold ? "600" : "400") + "; "
                + (centered ? "-fx-alignment: center;" : "");
        return new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v);
                if (!empty) setStyle(style);
            }
        };
    }

    private TableCell<StudentMarkSummary, String> scoreCell(String activeColor) {
        String activeStyle = "-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: " + activeColor + "; -fx-alignment: center;";
        String dashStyle   = "-fx-font-size: 13px; -fx-font-weight: 400; -fx-text-fill: #aaaaaa; -fx-alignment: center;";
        return new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(v);
                setStyle("—".equals(v) ? dashStyle : activeStyle);
            }
        };
    }

    /**
     * Generic status badge cell. statusColors maps known statuses to [bg, fg].
     * defaultColors is the fallback [bg, fg].
     */
    private <T> TableCell<T, String> statusBadgeCell(Map<String, String[]> statusColors,
                                                       String[] defaultColors) {
        return new TableCell<>() {
            private final Label badge = new Label();
            @Override protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null || "—".equals(status)) { setGraphic(null); setText(null); return; }
                badge.setText(status);
                String[] colors = statusColors.getOrDefault(status, defaultColors);
                badge.setStyle("-fx-background-color: " + colors[0] + "; -fx-text-fill: " + colors[1] + "; " +
                        "-fx-padding: 4 12 4 12; -fx-background-radius: 999; " +
                        "-fx-font-size: 12px; -fx-font-weight: 700;");
                setGraphic(badge); setText(null);
            }
        };
    }

    private <T> void setupGradeBadgeColumn(TableColumn<T, String> col) {
        col.setCellFactory(c -> new TableCell<>() {
            private final Label badge = new Label();
            @Override protected void updateItem(String grade, boolean empty) {
                super.updateItem(grade, empty);
                if (empty || grade == null) { setGraphic(null); setText(null); return; }
                badge.setText(grade);
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


    private static final String ACTIVE_STYLE =
            "-fx-background-color: #111111; -fx-background-radius: 8; -fx-text-fill: white; " +
            "-fx-font-size: 13px; -fx-font-weight: 700; -fx-padding: 10 14 10 14; -fx-cursor: hand;";
    private static final String INACTIVE_STYLE =
            "-fx-background-color: transparent; -fx-background-radius: 8; -fx-text-fill: #272727; " +
            "-fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 10 14 10 14; -fx-cursor: hand;";

    private void showPane(VBox target, Button activeBtn, String title, String subtitle) {
        for (VBox pane : List.of(dashboardPane, attendancePane, markSheetPane, reportCardPane, performancePane)) {
            boolean show = pane == target;
            pane.setVisible(show);
            pane.setManaged(show);
        }
        for (Button btn : List.of(navDashboardBtn, navAttendanceBtn, navMarkSheetBtn, navReportCardBtn, navPerformanceBtn)) {
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

    private void setAttStatus(String msg, boolean ok) {
        attStatusLabel.setText(msg);
        attStatusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: "
                + (ok ? "#16a34a" : "#dc2626") + ";");
    }
}
