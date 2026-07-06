package com.hamroschool.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bson.Document;

import com.hamroschool.config.MongoClientProvider;
import com.hamroschool.model.entity.SchoolClass;
import com.hamroschool.service.AuthService;
import com.hamroschool.service.ClassService;
import com.mongodb.client.MongoCollection;
import static com.mongodb.client.model.Filters.eq;

/**
 * MongoDB implementation of ClassService
 */
public class MongoClassService implements ClassService {
    
    private static MongoClassService instance;
    private final MongoCollection<Document> classCollection;
    private final AuthService authService;
    
    private MongoClassService() {
        MongoClientProvider provider = MongoClientProvider.getInstance();
        this.classCollection = provider.getDatabase().getCollection("classes");
        this.authService = MongoAuthService.getInstance();
    }
    
    public static synchronized MongoClassService getInstance() {
        if (instance == null) {
            instance = new MongoClassService();
        }
        return instance;
    }
    
    @Override
    public void createClass(String className, String createdBy) {
        if (classExists(className)) {
            throw new IllegalArgumentException("Class already exists: " + className);
        }
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        Document classDoc = new Document()
                .append("className", className)
                .append("assignedTeachers", new ArrayList<>())
                .append("enrolledStudents", new ArrayList<>())
                .append("createdBy", createdBy)
                .append("createdDate", timestamp);
        
        classCollection.insertOne(classDoc);
    }
    
    @Override
    public void updateClass(String oldClassName, String newClassName) {
        if (!classExists(oldClassName)) {
            throw new IllegalArgumentException("Class not found: " + oldClassName);
        }
        if (!oldClassName.equals(newClassName) && classExists(newClassName)) {
            throw new IllegalArgumentException("New class name already exists: " + newClassName);
        }
        
        Document update = new Document("$set", new Document("className", newClassName));
        classCollection.updateOne(eq("className", oldClassName), update);
    }
    
    @Override
    public void deleteClass(String className) {
        classCollection.deleteOne(eq("className", className));
    }
    
    @Override
    public List<SchoolClass> getAllClasses() {
        List<SchoolClass> classes = new ArrayList<>();
        for (Document doc : classCollection.find()) {
            classes.add(documentToSchoolClass(doc));
        }
        return classes;
    }
    
    @Override
    public Optional<SchoolClass> getClassByName(String className) {
        Document doc = classCollection.find(eq("className", className)).first();
        return doc != null ? Optional.of(documentToSchoolClass(doc)) : Optional.empty();
    }
    
    @Override
    public boolean classExists(String className) {
        return classCollection.find(eq("className", className)).first() != null;
    }
    
    @Override
    public void assignTeacher(String className, String teacherUsername) {
        Document update = new Document("$addToSet", 
                new Document("assignedTeachers", teacherUsername));
        classCollection.updateOne(eq("className", className), update);
    }
    
    @Override
    public void removeTeacher(String className, String teacherUsername) {
        Document update = new Document("$pull", 
                new Document("assignedTeachers", teacherUsername));
        classCollection.updateOne(eq("className", className), update);
    }
    
    @Override
    public void enrollStudent(String className, String studentUsername) {
        // First, remove student from any other class
        Optional<SchoolClass> currentClass = getClassByStudent(studentUsername);
        if (currentClass.isPresent() && !currentClass.get().getClassName().equals(className)) {
            removeStudent(currentClass.get().getClassName(), studentUsername);
        }
        
        Document update = new Document("$addToSet", 
                new Document("enrolledStudents", studentUsername));
        classCollection.updateOne(eq("className", className), update);
    }
    
    @Override
    public void removeStudent(String className, String studentUsername) {
        Document update = new Document("$pull", 
                new Document("enrolledStudents", studentUsername));
        classCollection.updateOne(eq("className", className), update);
    }
    
    @Override
    public List<SchoolClass> getClassesByTeacher(String teacherUsername) {
        List<SchoolClass> classes = new ArrayList<>();
        for (Document doc : classCollection.find()) {
            @SuppressWarnings("unchecked")
            List<String> teachers = (List<String>) doc.get("assignedTeachers");
            if (teachers != null && teachers.contains(teacherUsername)) {
                classes.add(documentToSchoolClass(doc));
            }
        }
        return classes;
    }
    
    @Override
    public Optional<SchoolClass> getClassByStudent(String studentUsername) {
        for (Document doc : classCollection.find()) {
            @SuppressWarnings("unchecked")
            List<String> students = (List<String>) doc.get("enrolledStudents");
            if (students != null && students.contains(studentUsername)) {
                return Optional.of(documentToSchoolClass(doc));
            }
        }
        return Optional.empty();
    }
    
    @Override
    public List<String> getUnassignedStudents() {
        List<String> allStudents = authService.getAllUsersByRole(com.hamroschool.model.auth.UserRole.STUDENT)
                .stream()
                .map(user -> user.getUsername())
                .collect(Collectors.toList());
        
        List<String> enrolledStudents = new ArrayList<>();
        for (Document doc : classCollection.find()) {
            @SuppressWarnings("unchecked")
            List<String> students = (List<String>) doc.get("enrolledStudents");
            if (students != null) {
                enrolledStudents.addAll(students);
            }
        }
        
        allStudents.removeAll(enrolledStudents);
        return allStudents;
    }
    
    @Override
    public List<String> getUnassignedTeachers() {
        List<String> allTeachers = authService.getAllUsersByRole(com.hamroschool.model.auth.UserRole.TEACHER)
                .stream()
                .map(user -> user.getUsername())
                .collect(Collectors.toList());
        
        List<String> assignedTeachers = new ArrayList<>();
        for (Document doc : classCollection.find()) {
            @SuppressWarnings("unchecked")
            List<String> teachers = (List<String>) doc.get("assignedTeachers");
            if (teachers != null) {
                assignedTeachers.addAll(teachers);
            }
        }
        
        allTeachers.removeAll(assignedTeachers);
        return allTeachers;
    }
    
    private SchoolClass documentToSchoolClass(Document doc) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setClassName(doc.getString("className"));
        schoolClass.setCreatedBy(doc.getString("createdBy"));
        schoolClass.setCreatedDate(doc.getString("createdDate"));
        
        @SuppressWarnings("unchecked")
        List<String> teachers = (List<String>) doc.get("assignedTeachers");
        if (teachers != null) {
            schoolClass.setAssignedTeachers(new ArrayList<>(teachers));
        }
        
        @SuppressWarnings("unchecked")
        List<String> students = (List<String>) doc.get("enrolledStudents");
        if (students != null) {
            schoolClass.setEnrolledStudents(new ArrayList<>(students));
        }
        
        return schoolClass;
    }
}
