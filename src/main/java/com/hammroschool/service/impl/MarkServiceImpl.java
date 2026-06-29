package com.hammroschool.service.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hammroschool.config.AppConfig;
import com.hammroschool.config.DatabaseSupport;
import com.hammroschool.model.auth.UserRole;
import com.hammroschool.model.dto.ReportCardEntry;
import com.hammroschool.model.dto.StudentMarkSummary;
import com.hammroschool.model.entity.Mark;
import com.hammroschool.service.MarkService;

public final class MarkServiceImpl implements MarkService {

    private static final MarkServiceImpl INSTANCE = new MarkServiceImpl();

    private final DatabaseSupport db;

    private MarkServiceImpl() {
        AppConfig cfg = AppConfig.getInstance();
        this.db = new DatabaseSupport(
                cfg.getDatabaseUrl(),
                cfg.getDatabaseUsername(),
                cfg.getDatabasePassword(),
                cfg.getDatabaseDriver());
        db.initializeSchemaIfNeeded();
    }

    public static MarkServiceImpl getInstance() {
        return INSTANCE;
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    @Override
    public synchronized long saveMark(String studentUsername, String subjectName,
                                      String teacherUsername, int score, int fullMarks,
                                      String examType, String remarks) {
        // Delete existing record for same key before inserting (H2 MERGE on non-PK columns)
        String deleteSql = "DELETE FROM marks WHERE student_username = ? AND subject_name = ? "
                         + "AND teacher_username = ? AND exam_type = ?";
        String insertSql = "INSERT INTO marks "
                         + "(student_username, subject_name, teacher_username, score, full_marks, exam_type, remarks) "
                         + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = db.openConnection()) {
            try (PreparedStatement del = con.prepareStatement(deleteSql)) {
                del.setString(1, studentUsername);
                del.setString(2, subjectName);
                del.setString(3, teacherUsername);
                del.setString(4, examType == null ? "Terminal" : examType);
                del.executeUpdate();
            }
            try (PreparedStatement ins = con.prepareStatement(insertSql,
                                                               new String[]{"ID"})) {
                ins.setString(1, studentUsername);
                ins.setString(2, subjectName);
                ins.setString(3, teacherUsername);
                ins.setInt(4, score);
                ins.setInt(5, fullMarks);
                ins.setString(6, examType == null ? "Terminal" : examType);
                ins.setString(7, remarks == null ? "" : remarks);
                ins.executeUpdate();
                try (ResultSet keys = ins.getGeneratedKeys()) {
                    return keys.next() ? keys.getLong(1) : -1;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save mark", e);
        }
    }

    @Override
    public synchronized void deleteMark(long id) {
        try (Connection con = db.openConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM marks WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete mark id=" + id, e);
        }
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    public synchronized List<Mark> getMarksByTeacher(String teacherUsername) {
        return query(
            "SELECT * FROM marks WHERE teacher_username = ? ORDER BY created_at DESC",
            teacherUsername);
    }

    @Override
    public synchronized List<Mark> getMarksByStudentAndTeacher(String studentUsername,
                                                                String teacherUsername) {
        String sql = "SELECT * FROM marks WHERE student_username = ? AND teacher_username = ? "
                   + "ORDER BY subject_name";
        try (Connection con = db.openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, studentUsername);
            ps.setString(2, teacherUsername);
            try (ResultSet rs = ps.executeQuery()) {
                return mapAll(rs);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query marks", e);
        }
    }

    @Override
    public synchronized List<String> getSubjectsByTeacher(String teacherUsername) {
        List<String> result = new ArrayList<>();
        String sql = "SELECT DISTINCT subject_name FROM marks "
                   + "WHERE teacher_username = ? ORDER BY subject_name";
        try (Connection con = db.openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, teacherUsername);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(rs.getString(1));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query subjects", e);
        }
        return result;
    }

    @Override
    public synchronized List<String> getAllStudentUsernames() {
        List<String> result = new ArrayList<>();
        String sql = "SELECT username FROM user_accounts WHERE role = ? ORDER BY username";
        try (Connection con = db.openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, UserRole.STUDENT.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(rs.getString(1));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query students", e);
        }
        return result;
    }

    // ── Marksheet aggregation ─────────────────────────────────────────────────

    @Override
    public synchronized List<StudentMarkSummary> getMarksheet(String teacherUsername,
                                                               String subjectName) {
        // Pivot: get midterm and final scores per student in one pass
        String sql =
            "SELECT student_username, exam_type, score, full_marks " +
            "FROM marks WHERE teacher_username = ? AND subject_name = ? " +
            "ORDER BY student_username";

        // student -> [midtermScore, finalScore, midtermFM, finalFM]
        Map<String, int[]> pivot = new LinkedHashMap<>();

        try (Connection con = db.openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, teacherUsername);
            ps.setString(2, subjectName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String student  = rs.getString("student_username");
                    String examType = rs.getString("exam_type").toLowerCase();
                    int score       = rs.getInt("score");
                    int fullMarks   = rs.getInt("full_marks");
                    int[] row = pivot.computeIfAbsent(student, k -> new int[]{-1, -1, 50, 50});
                    if (examType.contains("mid")) {
                        row[0] = score; row[2] = fullMarks;
                    } else {
                        row[1] = score; row[3] = fullMarks;
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to build marksheet", e);
        }

        // Build result — include ALL students (even those with no marks yet)
        List<String> allStudents = getAllStudentUsernames();
        List<StudentMarkSummary> result = new ArrayList<>();
        int roll = 1;
        for (String student : allStudents) {
            int[] row = pivot.getOrDefault(student, new int[]{-1, -1, 50, 50});
            int fm = (row[2] > 0 ? row[2] : 50) + (row[3] > 0 ? row[3] : 50);
            result.add(new StudentMarkSummary(roll++, student, row[0], row[1], fm));
        }
        return result;
    }

    @Override
    public synchronized double getAverageMarks(String teacherUsername, String subjectName) {
        List<StudentMarkSummary> sheet = getMarksheet(teacherUsername, subjectName);
        return sheet.stream()
                .filter(s -> s.getMidterm() >= 0 || s.getFinalMark() >= 0)
                .mapToDouble(StudentMarkSummary::getPercentage)
                .average().orElse(0);
    }

    @Override
    public synchronized double getPassRate(String teacherUsername, String subjectName) {
        List<StudentMarkSummary> sheet = getMarksheet(teacherUsername, subjectName);
        List<StudentMarkSummary> withMarks = sheet.stream()
                .filter(s -> s.getMidterm() >= 0 || s.getFinalMark() >= 0).toList();
        if (withMarks.isEmpty()) return 0;
        long passed = withMarks.stream().filter(s -> s.getPercentage() >= 40).count();
        return Math.round(passed * 1000.0 / withMarks.size()) / 10.0;
    }

    @Override
    public synchronized int getTopScore(String teacherUsername, String subjectName) {
        List<StudentMarkSummary> sheet = getMarksheet(teacherUsername, subjectName);
        return sheet.stream()
                .filter(s -> s.getMidterm() >= 0 || s.getFinalMark() >= 0)
                .mapToInt(StudentMarkSummary::getTotal)
                .max().orElse(0);
    }

    @Override
    public synchronized List<ReportCardEntry> getReportCard(String teacherUsername,
                                                             String subjectName) {
        List<StudentMarkSummary> sheet = getMarksheet(teacherUsername, subjectName);

        // Sort by percentage descending to assign ranks
        List<StudentMarkSummary> sorted = sheet.stream()
                .sorted((a, b) -> Double.compare(b.getPercentage(), a.getPercentage()))
                .toList();

        // Build rank map (students with same percentage get same rank)
        Map<String, Integer> rankMap = new java.util.LinkedHashMap<>();
        int rank = 1;
        for (int i = 0; i < sorted.size(); i++) {
            StudentMarkSummary s = sorted.get(i);
            if (i > 0 && sorted.get(i - 1).getPercentage() == s.getPercentage()) {
                rankMap.put(s.getUsername(), rankMap.get(sorted.get(i - 1).getUsername()));
            } else {
                rankMap.put(s.getUsername(), rank);
            }
            rank++;
        }

        // Return in original roll order
        List<ReportCardEntry> result = new java.util.ArrayList<>();
        for (StudentMarkSummary s : sheet) {
            int r = rankMap.getOrDefault(s.getUsername(), sheet.size());
            result.add(new ReportCardEntry(s.getRoll(), s.getUsername(), s.getPercentage(), r));
        }
        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<Mark> query(String sql, String param) {
        try (Connection con = db.openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return mapAll(rs);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query marks", e);
        }
    }

    private List<Mark> mapAll(ResultSet rs) throws SQLException {
        List<Mark> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    private Mark mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        return new Mark(
                rs.getLong("id"),
                rs.getString("student_username"),
                rs.getString("subject_name"),
                rs.getString("teacher_username"),
                rs.getInt("score"),
                rs.getInt("full_marks"),
                rs.getString("exam_type"),
                rs.getString("remarks"),
                ts != null ? ts.toLocalDateTime() : LocalDateTime.now());
    }
}
