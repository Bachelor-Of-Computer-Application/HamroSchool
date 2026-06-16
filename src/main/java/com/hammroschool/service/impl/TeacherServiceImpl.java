package com.hammroschool.service.impl;

import java.util.List;

import com.hammroschool.model.entity.Teacher;
import com.hammroschool.service.TeacherService;

public class TeacherServiceImpl implements TeacherService {

    private static final TeacherServiceImpl INSTANCE = new TeacherServiceImpl();

    private final List<Teacher> teachers = List.of(
        new Teacher(1L,  "Pramod",  "pramod",  "Mathematics",    true),
        new Teacher(2L,  "Rajesh",  "rajesh",  "Science",        true),
        new Teacher(3L,  "Kamala",  "kamala",  "English",        true),
        new Teacher(4L,  "Bikash",  "bikash",  "Nepali",         false),
        new Teacher(5L,  "Sarita",  "sarita",  "Social Studies", true),
        new Teacher(6L,  "Hari",    "hari",    "Mathematics",    true),
        new Teacher(7L,  "Sunita",  "sunita",  "Science",        false),
        new Teacher(8L,  "Ramesh",  "ramesh",  "English",        true),
        new Teacher(9L,  "Gita",    "gita",    "Nepali",         true),
        new Teacher(10L, "Sita",    "sita",    "Social Studies", true),
        new Teacher(11L, "Nabin",   "nabin",   "Mathematics",    true),
        new Teacher(12L, "Anita",   "anita",   "Science",        false),
        new Teacher(13L, "Dinesh",  "dinesh",  "English",        true),
        new Teacher(14L, "Puja",    "puja",    "Nepali",         true),
        new Teacher(15L, "Rajan",   "rajan",   "Social Studies", true),
        new Teacher(16L, "Maya",    "maya",    "Mathematics",    true),
        new Teacher(17L, "Suresh",  "suresh",  "Science",        false),
        new Teacher(18L, "Kabita",  "kabita",  "English",        true),
        new Teacher(19L, "Prakash", "prakash", "Nepali",         true),
        new Teacher(20L, "Lila",    "lila",    "Social Studies", true),
        new Teacher(21L, "Bijay",   "bijay",   "Mathematics",    true),
        new Teacher(22L, "Nisha",   "nisha",   "Science",        true),
        new Teacher(23L, "Anil",    "anil",    "English",        false),
        new Teacher(24L, "Sima",    "sima",    "Nepali",         true),
        new Teacher(25L, "Gopal",   "gopal",   "Social Studies", true),
        new Teacher(26L, "Rekha",   "rekha",   "Mathematics",    true),
        new Teacher(27L, "Bimal",   "bimal",   "Science",        true),
        new Teacher(28L, "Mina",    "mina",    "English",        false),
        new Teacher(29L, "Kamal",   "kamal",   "Nepali",         true),
        new Teacher(30L, "Rita",    "rita",    "Social Studies", true),
        new Teacher(31L, "Sanjay",  "sanjay",  "Mathematics",    true),
        new Teacher(32L, "Sabina",  "sabina",  "Science",        true),
        new Teacher(33L, "Dil",     "dil",     "English",        true),
        new Teacher(34L, "Hema",    "hema",    "Nepali",         false),
        new Teacher(35L, "Arjun",   "arjun",   "Social Studies", true),
        new Teacher(36L, "Kopila",  "kopila",  "Mathematics",    true),
        new Teacher(37L, "Roshan",  "roshan",  "Science",        true),
        new Teacher(38L, "Pabitra", "pabitra", "English",        true),
        new Teacher(39L, "Nirajan", "nirajan", "Nepali",         true),
        new Teacher(40L, "Sangita", "sangita", "Social Studies", false),
        new Teacher(41L, "Deepak",  "deepak",  "Mathematics",    true),
        new Teacher(42L, "Urmila",  "urmila",  "Science",        true)
    );

    private TeacherServiceImpl() {}

    public static TeacherServiceImpl getInstance() {
        return INSTANCE;
    }

    @Override
    public List<Teacher> getAllTeachers() {
        return teachers;
    }

    @Override
    public long countActive() {
        return teachers.stream().filter(Teacher::isActive).count();
    }

    @Override
    public long countNewThisMonth() {
        // In a real app this would query by join date; returning a fixed demo value
        return 6;
    }
}
