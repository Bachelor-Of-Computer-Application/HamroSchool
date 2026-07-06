package com.hamroschool.service;

import java.util.List;

import com.hamroschool.model.entity.SchoolClass;
import com.hamroschool.service.impl.MongoClassService;

/**
 * Service to initialize default data in the database
 */
public class DataInitializationService {
    
    private static DataInitializationService instance;
    private final ClassService classService;
    
    private DataInitializationService() {
        this.classService = MongoClassService.getInstance();
    }
    
    public static synchronized DataInitializationService getInstance() {
        if (instance == null) {
            instance = new DataInitializationService();
        }
        return instance;
    }
    
    /**
     * Initialize default classes (Class 1 to Class 10) if they don't exist
     */
    public void initializeDefaultClasses() {
        System.out.println("[DataInitialization] Checking default classes...");
        
        // Check which classes from 1-10 are missing
        for (int i = 1; i <= 10; i++) {
            String className = "Class " + i;
            
            if (!classService.classExists(className)) {
                try {
                    classService.createClass(className, "system");
                    System.out.println("[DataInitialization] Created: " + className);
                } catch (Exception e) {
                    System.err.println("[DataInitialization] Failed to create " + className + ": " + e.getMessage());
                }
            } else {
                System.out.println("[DataInitialization] " + className + " already exists.");
            }
        }
        
        List<SchoolClass> allClasses = classService.getAllClasses();
        System.out.println("[DataInitialization] Total classes in database: " + allClasses.size());
    }
    
    /**
     * Initialize all default data
     * Call this method when the application starts
     */
    public void initializeAllData() {
        initializeDefaultClasses();
    }
}
