package com.hammroschool.service;

import java.util.List;

import com.hammroschool.model.entity.Teacher;

public interface TeacherService {
    List<Teacher> getAllTeachers();
    long countActive();
    long countNewThisMonth();
}
