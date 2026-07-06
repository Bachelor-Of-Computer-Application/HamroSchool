package com.hamroschool.util;

import com.hamroschool.config.MongoClientProvider;
import com.hamroschool.service.ClassService;
import com.hamroschool.service.impl.MongoClassService;

public class SeedClasses {
    
    public static void main(String[] args) {
        MongoClientProvider.getInstance();
        
        ClassService classService = MongoClassService.getInstance();
        
        System.out.println("=== Seeding Classes 1-10 ===");
        
        for (int i = 1; i <= 10; i++) {
            String className = "Class " + i;
            
            try {
                if (classService.classExists(className)) {
                    System.out.println("[SKIP] " + className + " already exists");
                } else {
                    classService.createClass(className, "system");
                    System.out.println("[OK] Created " + className);
                }
            } catch (Exception e) {
                System.err.println("[ERROR] Failed to create " + className + ": " + e.getMessage());
            }
        }
        
        System.out.println("\n=== Summary ===");
        System.out.println("Total classes in database: " + classService.getAllClasses().size());
        System.out.println("\nClasses list:");
        classService.getAllClasses().forEach(c -> 
            System.out.println("  - " + c.getClassName() + 
                " (Teachers: " + c.getTeacherCount() + 
                ", Students: " + c.getStudentCount() + ")")
        );
        
        MongoClientProvider.getInstance().close();
        System.out.println("\nDone!");
    }
}
