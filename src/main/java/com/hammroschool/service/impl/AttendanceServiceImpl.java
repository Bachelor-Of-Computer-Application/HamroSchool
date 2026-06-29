package com.hammroschool.service.impl;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hammroschool.config.DatabaseSupport;
import com.hammroschool.model.entity.AttendanceRecord;
import com.hammroschool.service.AttendanceService;

public final class AttendanceServiceImpl implements AttendanceService {

    private static final AttendanceServiceImpl INSTANCE = new AttendanceServiceImpl();
    private final DatabaseSupport db = DatabaseSupport.getInstance();

    private AttendanceServiceImpl() {}

    public static AttendanceServiceImpl getInstance() {
        return INSTANCE;
    }

    @Override
    public synchronized void saveAttendance(String studentUsername, String teacherUsername,
                                             String subjectName, LocalDate date, String status) {
        String sql =
            "MERGE INTO attendance (student_username, teacher_username, subject_name, attendance_date, status) " +
            "KEY (student_username, teacher_username, attendance_date, subject_name) " +
            "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = db.openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, studentUsername);
            ps.setString(2, teacherUsername);
            ps.setString(3, subjectName);
            ps.setDate(4, Date.valueOf(date));
            ps.setString(5, status == null ? "PRESENT" : status.toUpperCase());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save attendance", e);
        }
    }

    @Override
    public synchronized List<AttendanceRecord> getAttendanceForDate(String teacherUsername,
                                                                      String subjectName,
                                                                      LocalDate date) {
        String sql =
            "SELECT * FROM attendance WHERE teacher_username = ? AND subject_name = ? " +
            "AND attendance_date = ? ORDER BY student_username";
        List<AttendanceRecord> result = new ArrayList<>();
        try (Connection con = db.openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, teacherUsername);
            ps.setString(2, subjectName);
            ps.setDate(3, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load attendance", e);
        }
        return result;
    }

    @Override
    public synchronized Map<String, Double> getAttendancePercentages(String teacherUsername,
                                                                       String subjectName) {
        // Count total days and present+late days per student
        String sql =
            "SELECT student_username, " +
            "COUNT(*) AS total, " +
            "SUM(CASE WHEN status IN ('PRESENT','LATE') THEN 1 ELSE 0 END) AS attended " +
            "FROM attendance WHERE teacher_username = ? AND subject_name = ? " +
            "GROUP BY student_username";
        Map<String, Double> map = new HashMap<>();
        try (Connection con = db.openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, teacherUsername);
            ps.setString(2, subjectName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int total    = rs.getInt("total");
                    int attended = rs.getInt("attended");
                    double pct   = total > 0 ? Math.round((attended * 1000.0) / total) / 10.0 : 0;
                    map.put(rs.getString("student_username"), pct);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to compute attendance percentages", e);
        }
        return map;
    }

    @Override
    public synchronized String getStatusForToday(String studentUsername, String teacherUsername,
                                                   String subjectName, LocalDate date) {
        String sql =
            "SELECT status FROM attendance WHERE student_username = ? AND teacher_username = ? " +
            "AND subject_name = ? AND attendance_date = ?";
        try (Connection con = db.openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, studentUsername);
            ps.setString(2, teacherUsername);
            ps.setString(3, subjectName);
            ps.setDate(4, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("status");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to get today's status", e);
        }
        return "PRESENT"; // default
    }

    private AttendanceRecord mapRow(ResultSet rs) throws SQLException {
        return new AttendanceRecord(
            rs.getLong("id"),
            rs.getString("student_username"),
            rs.getString("teacher_username"),
            rs.getString("subject_name"),
            rs.getDate("attendance_date").toLocalDate(),
            rs.getString("status")
        );
    }
}
