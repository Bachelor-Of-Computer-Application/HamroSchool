package com.hamroschool.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.hamroschool.model.entity.AttendanceRecord;

public interface AttendanceService {

    /** Save (upsert) attendance for one student on a given date. */
    void saveAttendance(String studentUsername, String teacherUsername,
                        String subjectName, LocalDate date, String status);

    /** Get all attendance records for a teacher on a specific date and subject. */
    List<AttendanceRecord> getAttendanceForDate(String teacherUsername,
                                                String subjectName, LocalDate date);

    /**
     * Returns a map of studentUsername → attendance percentage (0-100)
     * based on all records by this teacher for this subject.
     */
    Map<String, Double> getAttendancePercentages(String teacherUsername, String subjectName);

    /** Get the most recent status for a student (for today's row default). */
    String getStatusForToday(String studentUsername, String teacherUsername,
                             String subjectName, LocalDate date);
}
