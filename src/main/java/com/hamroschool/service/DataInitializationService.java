package com.hamroschool.service;

import com.hamroschool.service.impl.MongoClassService;

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
    
    public void initializeDefaultClasses() {
        int created = 0;
        int existing = 0;
        
        for (int i = 1; i <= 10; i++) {
            String className = "Class " + i;
            
            if (!classService.classExists(className)) {
                try {
                    classService.createClass(className, "system");
                    System.out.println("[DataInitialization] Created: " + className);
                    created++;
                } catch (Exception e) {
                    System.err.println("[DataInitialization] Failed to create " + className + ": " + e.getMessage());
                }
            } else {
                existing++;
            }
        }
        
        if (created > 0) {
            System.out.println("[DataInitialization] Successfully created " + created + " new class(es)");
        }
        
        if (created == 0 && existing > 0) {
            System.out.println("[DataInitialization] ✓ All default classes initialized");
        }
    }
    
    public void initializeAllData() {
        initializeDefaultClasses();
    }
}
