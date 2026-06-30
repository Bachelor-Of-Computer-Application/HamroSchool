package com.hamroschool.model.entity;

import java.time.LocalDateTime;

public class Mark {
    private final Long id;
    private final String studentUsername;
    private final String subjectName;
    private final String teacherUsername;
    private final int score;
    private final int fullMarks;
    private final String examType;
    private final String remarks;
    private final LocalDateTime createdAt;

    public Mark(Long id, String studentUsername, String subjectName,
                String teacherUsername, int score, int fullMarks,
                String examType, String remarks, LocalDateTime createdAt) {
        this.id = id;
        this.studentUsername = studentUsername;
        this.subjectName = subjectName;
        this.teacherUsername = teacherUsername;
        this.score = score;
        this.fullMarks = fullMarks;
        this.examType = examType;
        this.remarks = remarks;
        this.createdAt = createdAt;
    }

    public Long getId()                  { return id; }
    public String getStudentUsername()   { return studentUsername; }
    public String getSubjectName()       { return subjectName; }
    public String getTeacherUsername()   { return teacherUsername; }
    public int getScore()                { return score; }
    public int getFullMarks()            { return fullMarks; }
    public String getExamType()          { return examType; }
    public String getRemarks()           { return remarks; }
    public LocalDateTime getCreatedAt()  { return createdAt; }

    /** Percentage rounded to one decimal place. */
    public double getPercentage() {
        if (fullMarks == 0) return 0.0;
        return Math.round((score * 1000.0) / fullMarks) / 10.0;
    }

    /** Letter grade based on percentage. */
    public String getGrade() {
        double p = getPercentage();
        if (p >= 90) return "A+";
        if (p >= 80) return "A";
        if (p >= 70) return "B+";
        if (p >= 60) return "B";
        if (p >= 50) return "C";
        if (p >= 40) return "D";
        return "F";
    }
}
