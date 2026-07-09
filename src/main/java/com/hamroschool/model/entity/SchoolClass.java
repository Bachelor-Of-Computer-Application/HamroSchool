package com.hamroschool.model.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a class in the school (e.g., Grade 1, Grade 2, etc.)
 * Each class has a unique name, assigned teachers, and enrolled students.
 */
public class SchoolClass {
    private String className;
    private List<String> assignedTeachers;  
    private List<String> enrolledStudents; 
    private String createdDate;
    private String createdBy;

    public SchoolClass() {
        this.assignedTeachers = new ArrayList<>();
        this.enrolledStudents = new ArrayList<>();
    }

    public SchoolClass(String className, String createdBy, String createdDate) {
        this.className = className;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.assignedTeachers = new ArrayList<>();
        this.enrolledStudents = new ArrayList<>();
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<String> getAssignedTeachers() {
        return assignedTeachers;
    }

    public void setAssignedTeachers(List<String> assignedTeachers) {
        this.assignedTeachers = assignedTeachers;
    }

    public List<String> getEnrolledStudents() {
        return enrolledStudents;
    }

    public void setEnrolledStudents(List<String> enrolledStudents) {
        this.enrolledStudents = enrolledStudents;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public int getTeacherCount() {
        return assignedTeachers.size();
    }

    public int getStudentCount() {
        return enrolledStudents.size();
    }

    @Override
    public String toString() {
        return className;
    }
}
