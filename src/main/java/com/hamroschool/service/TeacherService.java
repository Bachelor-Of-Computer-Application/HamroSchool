package com.hamroschool.service;

import java.util.List;
import java.util.Optional;

import com.hamroschool.model.auth.UserAccount;

public interface TeacherService {
    List<UserAccount> getAllTeachers();

    /**
     * Saves (or updates) the subject for a teacher account.
     * Call this after creating the user_account for the teacher.
     */
    boolean saveTeacherSubject(String username, String subject);

    /**
     * Returns the subject assigned to the given teacher username, or empty if none.
     */
    Optional<String> getSubject(String username);
}
