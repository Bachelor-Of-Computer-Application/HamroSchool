package com.hamroschool.service;

import java.util.List;
import java.util.Optional;

import com.hamroschool.model.entity.SchoolClass;

/**
 * Service interface for managing school classes.
 */
public interface ClassService {
    
    /**
     * Create a new class
     */
    void createClass(String className, String createdBy);
    
    /**
     * Update a class name
     */
    void updateClass(String oldClassName, String newClassName);
    
    /**
     * Delete a class
     */
    void deleteClass(String className);
    
    /**
     * Get all classes
     */
    List<SchoolClass> getAllClasses();
    
    /**
     * Get a specific class by name
     */
    Optional<SchoolClass> getClassByName(String className);
    
    /**
     * Check if a class exists
     */
    boolean classExists(String className);
    
    /**
     * Assign a teacher to a class
     */
    void assignTeacher(String className, String teacherUsername);
    
    /**
     * Remove a teacher from a class
     */
    void removeTeacher(String className, String teacherUsername);
    
    /**
     * Enroll a student in a class
     */
    void enrollStudent(String className, String studentUsername);
    
    /**
     * Remove a student from a class
     */
    void removeStudent(String className, String studentUsername);
    
    /**
     * Get classes assigned to a teacher
     */
    List<SchoolClass> getClassesByTeacher(String teacherUsername);
    
    /**
     * Get the class a student is enrolled in
     */
    Optional<SchoolClass> getClassByStudent(String studentUsername);
    
    /**
     * Get all students not enrolled in any class
     */
    List<String> getUnassignedStudents();
    
    /**
     * Get all teachers not assigned to any class
     */
    List<String> getUnassignedTeachers();
}
