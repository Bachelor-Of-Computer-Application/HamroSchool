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
import com.hamroschool.util.TableCellFactories;
import com.hamroschool.util.TableUtils;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class StudentDashboardController {

    private final MarkService       markService       = MarkServiceImpl.getInstance();
    private final AttendanceService attendanceService = AttendanceServiceImpl.getInstance();
    private final TeacherService    teacherService    = TeacherServiceImpl.getInstance();

    private String studentUsername;

    private volatile List<Mark>          cachedMarks          = List.of();
    private volatile Map<String, String> cachedSubjectTeacher = Map.of();
    private volatile boolean             dataLoaded           = false;
    
    private volatile Map<String, List<Mark>> marksBySubject   = Map.of();
    private volatile List<AttendanceRow>     attendanceRows   = List.of();
    private volatile double                  cachedAvgGrade   = 0.0;
    private volatile double                  cachedAvgAtt     = 0.0;

    public record CourseRow(String subject, String teacher, double avgPct, String grade) {}
    public record AttendanceRow(String subject, String teacher, double pct, int presentDays, int absentDays, int lateDays) {}

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

    @FXML private HBox dashboardPane;
    @FXML private VBox gradesPane;
    @FXML private VBox attendancePane;
    
    @FXML private VBox recentGradesContainer;
    @FXML private VBox recentAttendanceContainer;
    @FXML private Label attTodayLabelDash;

    @FXML private TextField                      searchField;
    @FXML private TableView<CourseRow>           coursesTable;
    @FXML private TableColumn<CourseRow, String> cColCourse;
    @FXML private TableColumn<CourseRow, String> cColInstructor;
    @FXML private TableColumn<CourseRow, String> cColGrade;
    @FXML private Label                          coursesSummaryLabel;
    @FXML private Button                         prevButton;
    @FXML private Button                         nextButton;

    @FXML private TableView<Mark>           marksTable;
    @FXML private TableColumn<Mark, String> mColSubject;
    @FXML private TableColumn<Mark, String> mColTeacher;
    @FXML private TableColumn<Mark, String> mColExam;
    @FXML private TableColumn<Mark, String> mColScore;
    @FXML private TableColumn<Mark, String> mColGrade;
    @FXML private TableColumn<Mark, String> mColRemarks;
    @FXML private Label                     marksSummaryLabel;

    @FXML private Label gradeStatGPA;
    @FXML private Label gradeStatTotal;
    @FXML private Label gradeStatHighest;
    @FXML private Label gradeStatImproved;
    @FXML private VBox  subjectGradesContainer;

    @FXML private Label attTodayLabel;
    @FXML private Label attStatTotalLabel;
    @FXML private Label attStatPresentLabel;
    @FXML private Label attStatRateLabel;
    @FXML private VBox  subjectGridContainer;

    @FXML private Label detailStudentId;
    @FXML private Label detailFullName;
    @FXML private Label detailGender;
    @FXML private Label detailDob;
    @FXML private Label detailClass;
    @FXML private Label detailRollNo;
    @FXML private Label detailPhone;
    @FXML private Label detailEmail;
    @FXML private Label detailAddress;
    @FXML private Label detailGuardianName;
    @FXML private Label detailGuardianPhone;


    @FXML
    public void initialize() {
        studentUsername = SessionContext.getInstance().requireCurrentUser().getUsername();
        String displayName = Utils.formatName(studentUsername);
        welcomeSubLabel.setText("Overview of academic performance, attendance trends, and upcoming priorities.");
        userInitialsLabel.setText(Utils.initials(studentUsername));
        userNameLabel.setText(displayName);

        loadStudentDetails();

        showDashboard();

        Thread loader = new Thread(() -> {
            try {
                long startTime = System.currentTimeMillis();
                
                List<Mark> marks = markService.getMarksByStudent(studentUsername);

                java.util.LinkedHashMap<String, String> subTeach = new java.util.LinkedHashMap<>();
                java.util.LinkedHashMap<String, Double> attMap   = new java.util.LinkedHashMap<>();
                java.util.LinkedHashMap<String, int[]> statusCountMap = new java.util.LinkedHashMap<>();

                for (var teacher : teacherService.getAllTeachers()) {
                    String tName   = teacher.getUsername();
                    String subject = teacherService.getSubject(tName).orElse(null);
                    if (subject == null || subject.isBlank()) continue;
                    subTeach.putIfAbsent(subject, tName);

                    Double pct = attendanceService
                            .getAttendancePercentages(tName, subject)
                            .get(studentUsername);
                    if (pct != null) {
                        attMap.put(subject, pct);

                        int[] totals = attendanceService.getAttendanceTotals(tName, subject)
                                .getOrDefault(studentUsername, new int[]{0, 0});
                        int totalDays   = totals[1];
                        int attendedDays = totals[0]; // present + late
                        int absentDays  = totalDays - attendedDays;
                        statusCountMap.put(subject, new int[]{attendedDays, absentDays, 0});
                    }
                }

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
                    int[] pal = statusCountMap.getOrDefault(subject, new int[]{0, 0, 0});
                    attRows.add(new AttendanceRow(subject, teacher, e.getValue(), pal[0], pal[1], pal[2]));
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
                    loadDashboardView();
                });
            } catch (Exception ex) {
                System.err.println("[StudentDashboard] Data load error: " + ex.getMessage());
            }
        }, "StudentDataLoader");
        loader.setDaemon(true);
        loader.start();
    }


    private void loadStudentDetails() {
        var user = SessionContext.getInstance().requireCurrentUser();
        
        detailStudentId.setText(user.getUserId().isEmpty() ? "—" : user.getUserId());
        detailFullName.setText(user.getFullName().isEmpty() ? "—" : user.getFullName());
        detailGender.setText(user.getGender().isEmpty() ? "—" : user.getGender());
        detailDob.setText(user.getDateOfBirth().isEmpty() ? "—" : user.getDateOfBirth());
        detailClass.setText(user.getAssignedClass().isEmpty() ? "—" : user.getAssignedClass());
        detailRollNo.setText(user.getRollNumber().isEmpty() ? "—" : user.getRollNumber());
        detailPhone.setText(user.getPhone().isEmpty() ? "—" : user.getPhone());
        detailEmail.setText(user.getEmail().isEmpty() ? "—" : user.getEmail());
        detailAddress.setText(user.getAddress().isEmpty() ? "—" : user.getAddress());
        detailGuardianName.setText(user.getGuardianName().isEmpty() ? "—" : user.getGuardianName());
        detailGuardianPhone.setText(user.getGuardianPhone().isEmpty() ? "—" : user.getGuardianPhone());
    }

    @FXML private void handleNavDashboard()  { showDashboard(); }
    @FXML private void handleNavGrades()     { showGrades(); }
    @FXML private void handleNavAttendance() { showAttendance(); }

    @FXML
    private void handleLogout() {
        SessionContext.getInstance().clear();
        SceneSwitcher.clearCache();
        SceneSwitcher.showView(logoutButton, "/com/hamroschool/hello-view.fxml", "Hamro School", SceneSwitcher.LOGIN_WIDTH, SceneSwitcher.LOGIN_HEIGHT);
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
        if (dataLoaded) {
            refreshStats();
            loadDashboardView();
        }
    }

    private void showGrades() {
        setPane(gradesPane);
        setActiveNav(navGradesBtn);
        loadGradesView();
    }

    private void showAttendance() {
        setPane(attendancePane);
        setActiveNav(navAttendanceBtn);
        loadAttendance();
    }

    private void setPane(javafx.scene.layout.Region target) {
        dashboardPane.setVisible(dashboardPane == target);
        dashboardPane.setManaged(dashboardPane == target);
        gradesPane.setVisible(gradesPane == target);
        gradesPane.setManaged(gradesPane == target);
        attendancePane.setVisible(attendancePane == target);
        attendancePane.setManaged(attendancePane == target);
    }

    private void setActiveNav(Button active) {
        String on  = "-fx-background-color: #111111; -fx-background-radius: 8; -fx-text-fill: white; "
                   + "-fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 0 12 0 12; -fx-cursor: hand;";
        String off = "-fx-background-color: transparent; -fx-background-radius: 8; -fx-text-fill: #44403c; "
                   + "-fx-font-size: 13px; -fx-font-weight: 500; -fx-padding: 0 12 0 12; -fx-cursor: hand;";
        for (Button b : List.of(navDashboardBtn, navGradesBtn, navAttendanceBtn))
            b.setStyle(b == active ? on : off);
    }


    private void refreshStats() {
        if (!dataLoaded) return;

        java.util.LinkedHashSet<String> subjects = new java.util.LinkedHashSet<>(cachedSubjectTeacher.keySet());
        subjects.addAll(marksBySubject.keySet());
        statSubjectsLabel.setText(String.valueOf(subjects.size()));

        statGradeLabel.setText(cachedAvgGrade >= 0 ? TableUtils.percentageToGrade(cachedAvgGrade) : "—");

        statAttLabel.setText(cachedAvgAtt >= 0 ? String.format("%.0f%%", cachedAvgAtt) : "—");
    }


    private void loadDashboardView() {
        if (!dataLoaded) return;

        recentGradesContainer.getChildren().clear();
        
        List<String> recentSubjects = marksBySubject.keySet().stream()
                .limit(3)
                .toList();
        
        for (String subject : recentSubjects) {
            List<Mark> marks = marksBySubject.get(subject);
            if (marks.isEmpty()) continue;
            
            double avg = marks.stream().mapToDouble(Mark::getPercentage).average().orElse(0);
            String grade = TableUtils.percentageToGrade(avg);
            String desc = marks.get(0).getExamType() + " average";
            
            HBox gradeRow = createGradeRow(subject, desc, grade);
            recentGradesContainer.getChildren().add(gradeRow);
        }
        
        if (recentGradesContainer.getChildren().isEmpty()) {
            Label empty = new Label("No grades recorded yet");
            empty.setStyle("-fx-text-fill: #78716c; -fx-font-size: 13px;");
            recentGradesContainer.getChildren().add(empty);
        }

        recentAttendanceContainer.getChildren().clear();
        
        List<AttendanceRow> recentAtt = attendanceRows.stream()
                .limit(4)
                .toList();
        
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy");
        attTodayLabelDash.setText(java.time.LocalDate.now().format(fmt));
        
        for (AttendanceRow att : recentAtt) {
            HBox attRow = createAttendanceRow(att.subject(), att.teacher(), getAttendanceStatus(att.pct()));
            recentAttendanceContainer.getChildren().add(attRow);
        }
        
        if (recentAttendanceContainer.getChildren().isEmpty()) {
            Label empty = new Label("No attendance records yet");
            empty.setStyle("-fx-text-fill: #78716c; -fx-font-size: 13px;");
            recentAttendanceContainer.getChildren().add(empty);
        }
    }

    private HBox createGradeRow(String subject, String description, String grade) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #fafaf9; -fx-background-radius: 10; -fx-padding: 14 16; -fx-border-color: #e7e5e4; -fx-border-radius: 10;");

        String icon = TableUtils.getSubjectIcon(subject);
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 20px; -fx-min-width: 36; -fx-alignment: center;");
        
        VBox textBox = new VBox(2);
        textBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textBox, javafx.scene.layout.Priority.ALWAYS);
        
        Label subjectLabel = new Label(subject);
        subjectLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #111111;");
        
        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #78716c;");
        
        textBox.getChildren().addAll(subjectLabel, descLabel);
        
        Label gradeBadge = new Label(grade);
        String gradeColor = getGradeColor(grade);
        gradeBadge.setStyle("-fx-background-color: " + gradeColor + "; -fx-text-fill: " + getGradeTextColor(grade) + "; " +
                           "-fx-font-size: 13px; -fx-font-weight: 800; -fx-padding: 6 16; -fx-background-radius: 8; -fx-min-width: 50; -fx-alignment: center;");
        
        row.getChildren().addAll(iconLabel, textBox, gradeBadge);
        return row;
    }

    private HBox createAttendanceRow(String subject, String teacher, String status) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #fafaf9; -fx-background-radius: 10; -fx-padding: 14 16; -fx-border-color: #e7e5e4; -fx-border-radius: 10;");

        VBox textBox = new VBox(2);
        textBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textBox, javafx.scene.layout.Priority.ALWAYS);
        
        Label subjectLabel = new Label(subject);
        subjectLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #111111;");
        
        Label teacherLabel = new Label(teacher);
        teacherLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #78716c;");
        
        textBox.getChildren().addAll(subjectLabel, teacherLabel);
        
        Label statusBadge = new Label(status);
        String[] colors = getStatusColors(status);
        statusBadge.setStyle("-fx-background-color: " + colors[0] + "; -fx-text-fill: " + colors[1] + "; " +
                            "-fx-font-size: 12px; -fx-font-weight: 700; -fx-padding: 6 16; -fx-background-radius: 999; -fx-min-width: 70; -fx-alignment: center;");
        
        row.getChildren().addAll(textBox, statusBadge);
        return row;
    }

    private String getAttendanceStatus(double percentage) {
        if (percentage >= 75) return "Present";
        if (percentage >= 50) return "Late";
        return "Absent";
    }

    private String[] getStatusColors(String status) {
        return switch (status) {
            case "Present" -> new String[]{"#dcfce7", "#16a34a"};
            case "Late" -> new String[]{"#fef9c3", "#a16207"};
            case "Absent" -> new String[]{"#fee2e2", "#dc2626"};
            default -> new String[]{"#f5f5f4", "#78716c"};
        };
    }

    private String getGradeColor(String grade) {
        if (grade.startsWith("A")) return "#dcfce7";
        if (grade.startsWith("B")) return "#dbeafe";
        if (grade.startsWith("C")) return "#fef9c3";
        return "#fee2e2";
    }

    private String getGradeTextColor(String grade) {
        if (grade.startsWith("A")) return "#16a34a";
        if (grade.startsWith("B")) return "#2563eb";
        if (grade.startsWith("C")) return "#a16207";
        return "#dc2626";
    }


    private void loadCourses() {
        if (!dataLoaded) return;

        java.util.LinkedHashSet<String> allSubjects = new java.util.LinkedHashSet<>(cachedSubjectTeacher.keySet());
        allSubjects.addAll(marksBySubject.keySet());

        List<CourseRow> rows = new ArrayList<>();
        for (String subject : allSubjects) {
            List<Mark> marks = marksBySubject.getOrDefault(subject, List.of());
            double avg    = marks.isEmpty() ? 0 : marks.stream().mapToDouble(Mark::getPercentage).average().orElse(0);
            String teacher = marks.isEmpty() ? cachedSubjectTeacher.getOrDefault(subject, "—") : marks.get(0).getTeacherUsername();
            String grade   = marks.isEmpty() ? "—" : TableUtils.percentageToGrade(avg);
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


    private void loadGradesView() {
        if (!dataLoaded) return;

        // Calculate stats
        java.util.LinkedHashSet<String> subjects = new java.util.LinkedHashSet<>(cachedSubjectTeacher.keySet());
        subjects.addAll(marksBySubject.keySet());
        
        gradeStatTotal.setText(String.valueOf(subjects.size()));
        gradeStatGPA.setText(cachedAvgGrade >= 0 ? TableUtils.percentageToGrade(cachedAvgGrade) : "—");
        
        // Find highest grade
        String highestGrade = "—";
        if (!cachedMarks.isEmpty()) {
            double maxPct = cachedMarks.stream().mapToDouble(Mark::getPercentage).max().orElse(0);
            highestGrade = TableUtils.percentageToGrade(maxPct);
        }
        gradeStatHighest.setText(highestGrade);
        
        gradeStatImproved.setText("—");

        subjectGradesContainer.getChildren().clear();
        
        List<String> subjectList = new ArrayList<>(subjects);
        for (int i = 0; i < subjectList.size(); i += 2) {
            HBox row = new HBox(14);
            
            String subject1 = subjectList.get(i);
            VBox card1 = createSubjectGradeCard(subject1);
            card1.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(card1, javafx.scene.layout.Priority.ALWAYS);
            row.getChildren().add(card1);
            
            if (i + 1 < subjectList.size()) {
                String subject2 = subjectList.get(i + 1);
                VBox card2 = createSubjectGradeCard(subject2);
                card2.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(card2, javafx.scene.layout.Priority.ALWAYS);
                row.getChildren().add(card2);
            } else {
                Region spacer = new Region();
                spacer.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                row.getChildren().add(spacer);
            }
            
            subjectGradesContainer.getChildren().add(row);
        }
        
        if (subjects.isEmpty()) {
            Label empty = new Label("No grades recorded yet");
            empty.setStyle("-fx-text-fill: #78716c; -fx-font-size: 13px; -fx-padding: 20;");
            subjectGradesContainer.getChildren().add(empty);
        }
    }

    private VBox createSubjectGradeCard(String subject) {
        VBox card = new VBox(14);
        card.setStyle("-fx-background-color: #fafaf9; -fx-background-radius: 12; " +
                      "-fx-border-color: #e7e5e4; -fx-border-radius: 12; -fx-padding: 18;");

        // Get marks for this subject
        List<Mark> marks = marksBySubject.getOrDefault(subject, List.of());
        String teacher = marks.isEmpty() ? cachedSubjectTeacher.getOrDefault(subject, "—") 
                                         : marks.get(0).getTeacherUsername();
        
        // Determine subject type/category
        String category = getSubjectCategory(subject);
        
        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        
        VBox titleBox = new VBox(4);
        Label subjectName = new Label(subject);
        subjectName.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #111111;");
        Label teacherInfo = new Label(getTeacherInfo(teacher, subject));
        teacherInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: #78716c;");
        titleBox.getChildren().addAll(subjectName, teacherInfo);
        HBox.setHgrow(titleBox, javafx.scene.layout.Priority.ALWAYS);
        
        Label categoryBadge = new Label(category);
        categoryBadge.setStyle("-fx-background-color: #f5f5f4; -fx-text-fill: #78716c; " +
                               "-fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 4 10; " +
                               "-fx-background-radius: 6;");
        
        header.getChildren().addAll(titleBox, categoryBadge);
        
        HBox examCards = new HBox(8);
        
        Map<String, List<Mark>> marksByExam = marks.stream()
                .collect(Collectors.groupingBy(Mark::getExamType));
        
        String[] examTypes = {"Midterm", "Assignment", "Final"};
        for (String examType : examTypes) {
            List<Mark> examMarks = marksByExam.get(examType);
            VBox examCard = createExamTypeCard(examType, examMarks);
            examCard.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(examCard, javafx.scene.layout.Priority.ALWAYS);
            examCards.getChildren().add(examCard);
        }
        
        card.getChildren().addAll(header, examCards);
        return card;
    }

    private VBox createExamTypeCard(String examType, List<Mark> marks) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        
        String grade = "—";
        String bgColor = "#ffffff";
        String textColor = "#78716c";
        String icon = "";
        
        if (marks != null && !marks.isEmpty()) {
            double avg = marks.stream().mapToDouble(Mark::getPercentage).average().orElse(0);
            grade = TableUtils.percentageToGrade(avg);
            
            if (examType.equals("Midterm")) {
                icon = "✓";
                if (grade.startsWith("A")) {
                    bgColor = "#d1fae5";
                    textColor = "#065f46";
                } else if (grade.startsWith("B")) {
                    bgColor = "#d1fae5";
                    textColor = "#047857";
                } else if (grade.startsWith("C")) {
                    bgColor = "#fef9c3";
                    textColor = "#92400e";
                } else {
                    bgColor = "#fee2e2";
                    textColor = "#991b1b";
                }
            } else if (examType.equals("Assignment")) {
                icon = "📄";
                if (grade.startsWith("A")) {
                    bgColor = "#fef3c7";
                    textColor = "#78350f";
                } else if (grade.startsWith("B")) {
                    bgColor = "#fef3c7";
                    textColor = "#92400e";
                } else if (grade.startsWith("C")) {
                    bgColor = "#fef3c7";
                    textColor = "#a16207";
                } else {
                    bgColor = "#fee2e2";
                    textColor = "#991b1b";
                }
            } else { // Final
                icon = "🎓";
                if (grade.startsWith("A")) {
                    bgColor = "#cffafe";
                    textColor = "#155e75";
                } else if (grade.startsWith("B")) {
                    bgColor = "#cffafe";
                    textColor = "#0e7490";
                } else if (grade.startsWith("C")) {
                    bgColor = "#fef9c3";
                    textColor = "#92400e";
                } else {
                    bgColor = "#fecdd3";
                    textColor = "#be123c";
                }
            }
        } else {
            bgColor = "#f5f5f4";
            textColor = "#a8a29e";
            icon = examType.equals("Midterm") ? "✓" : examType.equals("Assignment") ? "📄" : "🎓";
        }
        
        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 10; -fx-padding: 14;");
        
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: " + textColor + ";");
        
        Label typeLabel = new Label(examType);
        typeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: " + textColor + ";");
        
        Label gradeLabel = new Label(grade);
        gradeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: " + textColor + ";");
        
        card.getChildren().addAll(iconLabel, typeLabel, gradeLabel);
        return card;
    }

    private String getSubjectCategory(String subject) {
        String lower = subject.toLowerCase();
        if (lower.contains("math") || lower.contains("science") || lower.contains("physics") || 
            lower.contains("chemistry") || lower.contains("biology")) {
            return "Core";
        } else if (lower.contains("english") || lower.contains("nepali")) {
            return "Language";
        } else if (lower.contains("computer") || lower.contains("it")) {
            return "Elective";
        } else if (lower.contains("social") || lower.contains("history") || lower.contains("geography")) {
            return "Core";
        }
        return "Other";
    }

    private String getTeacherInfo(String teacherUsername, String subject) {
        String formattedName = Utils.formatName(teacherUsername);
        String location = getClassroomLocation(subject);
        return location + " • " + formattedName;
    }

    private String getClassroomLocation(String subject) {
        int hash = Math.abs(subject.hashCode());
        int roomNum = 100 + (hash % 150);
        
        if (subject.toLowerCase().contains("math") || subject.toLowerCase().contains("science")) {
            return "Room " + roomNum;
        } else if (subject.toLowerCase().contains("english") || subject.toLowerCase().contains("language")) {
            return "Block " + (char)('A' + (hash % 5));
        } else if (subject.toLowerCase().contains("computer")) {
            return "Lab " + (1 + (hash % 3));
        }
        return "Room " + roomNum;
    }

    private void loadMarks() {
        if (!dataLoaded) return;
        marksTable.setItems(FXCollections.observableArrayList(cachedMarks));
        marksSummaryLabel.setText(cachedMarks.isEmpty()
                ? "No marks recorded yet."
                : "Showing " + cachedMarks.size() + " record" + (cachedMarks.size() == 1 ? "" : "s"));
    }

    private void setupGradesTable() {
        mColSubject.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getSubjectName()));
        mColSubject.setCellFactory(col -> TableCellFactories.plainTextCell("#111111", true));

        mColTeacher.setCellValueFactory(c -> new ReadOnlyStringWrapper(Utils.formatName(c.getValue().getTeacherUsername())));
        mColTeacher.setCellFactory(col -> TableCellFactories.plainTextCell("#44403c", false));

        mColExam.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getExamType()));
        mColExam.setCellFactory(col -> TableCellFactories.plainTextCell("#44403c", false));

        mColScore.setCellValueFactory(c -> {
            Mark m = c.getValue();
            return new ReadOnlyStringWrapper(m.getScore() + " / " + m.getFullMarks()
                    + "  (" + m.getPercentage() + "%)");
        });
        mColScore.setCellFactory(col -> TableCellFactories.plainTextCell("#111111", true));

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
        mColRemarks.setCellFactory(col -> TableCellFactories.plainTextCell("#78716c", false));

        marksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        marksTable.setPlaceholder(new Label("No marks recorded yet."));
        marksTable.setStyle("-fx-background-color: transparent;");
    }


    private void loadAttendance() {
        if (!dataLoaded) return;
        
        int totalSubjects = attendanceRows.size();
        long presentCount = attendanceRows.stream().filter(r -> r.pct() >= 75).count();
        double avgRate = attendanceRows.isEmpty() ? 0 :
            attendanceRows.stream().mapToDouble(AttendanceRow::pct).average().orElse(0);
        
        attStatTotalLabel.setText(String.valueOf(totalSubjects));
        attStatPresentLabel.setText(String.valueOf(presentCount));
        attStatRateLabel.setText(String.format("%.0f%%", avgRate));

        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy");
        attTodayLabel.setText(java.time.LocalDate.now().format(fmt));

        subjectGridContainer.getChildren().clear();

        for (int i = 0; i < attendanceRows.size(); i += 2) {
            HBox row = new HBox(14);

            AttendanceRow att1 = attendanceRows.get(i);
            VBox card1 = createSubjectCard(att1);
            card1.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(card1, javafx.scene.layout.Priority.ALWAYS);
            row.getChildren().add(card1);

            if (i + 1 < attendanceRows.size()) {
                AttendanceRow att2 = attendanceRows.get(i + 1);
                VBox card2 = createSubjectCard(att2);
                card2.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(card2, javafx.scene.layout.Priority.ALWAYS);
                row.getChildren().add(card2);
            } else {
                Region spacer = new Region();
                spacer.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                row.getChildren().add(spacer);
            }

            subjectGridContainer.getChildren().add(row);
        }

        if (attendanceRows.isEmpty()) {
            Label empty = new Label("No attendance records found.");
            empty.setStyle("-fx-text-fill: #78716c; -fx-font-size: 13px; -fx-padding: 20;");
            subjectGridContainer.getChildren().add(empty);
        }
    }

    private VBox createSubjectCard(AttendanceRow att) {
        VBox card = new VBox(14);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                      "-fx-border-color: #e7e5e4; -fx-border-radius: 12; -fx-padding: 18; " +
                      "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.04),8,0,0,2);");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label subjectName = new Label(att.subject());
        subjectName.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #111111;");
        HBox.setHgrow(subjectName, javafx.scene.layout.Priority.ALWAYS);

        String badgeText = att.pct() >= 75 ? "Good" : att.pct() >= 50 ? "At Risk" : "Critical";
        String badgeBg   = att.pct() >= 75 ? "#dcfce7" : att.pct() >= 50 ? "#fef9c3" : "#fee2e2";
        String badgeFg   = att.pct() >= 75 ? "#16a34a" : att.pct() >= 50 ? "#a16207" : "#dc2626";
        Label badge = new Label(badgeText);
        badge.setStyle("-fx-background-color: " + badgeBg + "; -fx-text-fill: " + badgeFg + "; " +
                       "-fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 4 10; " +
                       "-fx-background-radius: 999;");
        header.getChildren().addAll(subjectName, badge);

        HBox tiles = new HBox(8);

        VBox presentTile = createStatTile("✓", "Present",
                att.presentDays() + " days",
                "#e8f5e9", "#16a34a");
        VBox absentTile = createStatTile("✕", "Absent",
                att.absentDays() + " days",
                "#ffebee", "#dc2626");
        VBox lateTile = createStatTile("⏱", "Late",
                att.lateDays() + " days",
                "#fff9e6", "#d97706");

        for (VBox tile : new VBox[]{presentTile, absentTile, lateTile}) {
            tile.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(tile, javafx.scene.layout.Priority.ALWAYS);
        }
        tiles.getChildren().addAll(presentTile, absentTile, lateTile);

        card.getChildren().addAll(header, tiles);
        return card;
    }

    private VBox createStatTile(String icon, String label, String days,
                                String bgColor, String fgColor) {
        VBox tile = new VBox(6);
        tile.setAlignment(Pos.CENTER);
        tile.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 10; -fx-padding: 14 8;");

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: " + fgColor + ";");
        iconLabel.setAlignment(Pos.CENTER);
        iconLabel.setMaxWidth(Double.MAX_VALUE);

        Label textLabel = new Label(label);
        textLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: " + fgColor + ";");
        textLabel.setAlignment(Pos.CENTER);
        textLabel.setMaxWidth(Double.MAX_VALUE);

        Label daysLabel = new Label(days);
        daysLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #78716c;");
        daysLabel.setAlignment(Pos.CENTER);
        daysLabel.setMaxWidth(Double.MAX_VALUE);

        tile.getChildren().addAll(iconLabel, textLabel, daysLabel);
        return tile;
    }

}
