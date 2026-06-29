package com.hammroschool.model.entity;

import java.time.LocalDate;

public class AttendanceRecord {
    private final Long id;
    private final String studentUsername;
    private final String teacherUsername;
    private final String subjectName;
    private final LocalDate attendanceDate;
    private final String status; // PRESENT, ABSENT, LATE

    public AttendanceRecord(Long id, String studentUsername, String teacherUsername,
                            String subjectName, LocalDate attendanceDate, String status) {
        this.id = id;
        this.studentUsername = studentUsername;
        this.teacherUsername = teacherUsername;
        this.subjectName = subjectName;
        this.attendanceDate = attendanceDate;
        this.status = status;
    }

    public Long getId()                  { return id; }
    public String getStudentUsername()   { return studentUsername; }
    public String getTeacherUsername()   { return teacherUsername; }
    public String getSubjectName()       { return subjectName; }
    public LocalDate getAttendanceDate() { return attendanceDate; }
    public String getStatus()            { return status; }

    public boolean isPresent() { return "PRESENT".equals(status); }
    public boolean isAbsent()  { return "ABSENT".equals(status); }
    public boolean isLate()    { return "LATE".equals(status); }
}
