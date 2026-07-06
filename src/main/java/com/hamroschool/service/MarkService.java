package com.hamroschool.service;

import java.util.List;

import com.hamroschool.model.dto.ReportCardEntry;
import com.hamroschool.model.dto.StudentMarkSummary;
import com.hamroschool.model.entity.Mark;

public interface MarkService {

    long saveMark(String studentUsername, String subjectName, String teacherUsername,
                  int score, int fullMarks, String examType, String remarks);

    List<Mark> getMarksByTeacher(String teacherUsername);

    List<Mark> getMarksByStudentAndTeacher(String studentUsername, String teacherUsername);

    List<Mark> getMarksByStudent(String studentUsername);

    void deleteMark(long id);

    List<String> getSubjectsByTeacher(String teacherUsername);

    List<String> getAllStudentUsernames();
    List<StudentMarkSummary> getMarksheet(String teacherUsername, String subjectName);

    double getAverageMarks(String teacherUsername, String subjectName);

    double getPassRate(String teacherUsername, String subjectName);

    int getTopScore(String teacherUsername, String subjectName);

    List<ReportCardEntry> getReportCard(String teacherUsername, String subjectName);


    void assignSubject(String teacherUsername, String subjectName);

    void removeSubject(String teacherUsername, String subjectName);

    List<String> getAssignedSubjects(String teacherUsername);
}
