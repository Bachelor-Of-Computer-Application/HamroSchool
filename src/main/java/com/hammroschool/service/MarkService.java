package com.hammroschool.service;

import java.util.List;

import com.hammroschool.model.dto.ReportCardEntry;
import com.hammroschool.model.dto.StudentMarkSummary;
import com.hammroschool.model.entity.Mark;

public interface MarkService {

    /** Save or update a mark (MERGE on student+subject+teacher+examType). Returns generated id. */
    long saveMark(String studentUsername, String subjectName, String teacherUsername,
                  int score, int fullMarks, String examType, String remarks);

    /** All marks entered by a specific teacher, newest first. */
    List<Mark> getMarksByTeacher(String teacherUsername);

    /** All marks for a specific student entered by a specific teacher. */
    List<Mark> getMarksByStudentAndTeacher(String studentUsername, String teacherUsername);

    /** Delete a mark by id. */
    void deleteMark(long id);

    /** Distinct subject names used by a teacher. */
    List<String> getSubjectsByTeacher(String teacherUsername);

    /** All student usernames (STUDENT role). */
    List<String> getAllStudentUsernames();

    /**
     * Returns per-student summary (midterm, final, total, status) for the given
     * teacher and subject — one row per student who has at least one mark, plus
     * a blank row for every student account that has no mark yet.
     */
    List<StudentMarkSummary> getMarksheet(String teacherUsername, String subjectName);

    /** Stats: average percentage across all students with marks for this teacher+subject. */
    double getAverageMarks(String teacherUsername, String subjectName);

    /** Stats: pass rate (percentage ≥ 40) for this teacher+subject. */
    double getPassRate(String teacherUsername, String subjectName);

    /** Stats: top (highest total) score for this teacher+subject. */
    int getTopScore(String teacherUsername, String subjectName);

    /**
     * Returns a ranked report-card list for all students for the given
     * teacher+subject (students without marks get percentage 0 and rank last).
     */
    List<ReportCardEntry> getReportCard(String teacherUsername, String subjectName);
}
