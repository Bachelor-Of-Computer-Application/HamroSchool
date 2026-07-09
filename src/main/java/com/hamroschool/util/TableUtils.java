package com.hamroschool.util;

import javafx.scene.control.Label;
import javafx.scene.control.TableView;

public final class TableUtils {
    
    private TableUtils() {} 

    /**
     * Configure table with common settings
     * @param table The table to configure
     * @param placeholderText Text to show when table is empty
     */
    public static <T> void configureTable(TableView<T> table, String placeholderText) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label(placeholderText));
        table.setStyle("-fx-background-color: transparent;");
    }

    /**
     * Configure table with default empty message
     * @param table The table to configure
     */
    public static <T> void configureTable(TableView<T> table) {
        configureTable(table, "No data available");
    }


    /**
     * Get subject icon emoji
     * @param subject The subject name
     * @return Icon emoji
     */
    public static String getSubjectIcon(String subject) {
        if (subject == null) return "📚";
        String s = subject.toLowerCase(java.util.Locale.ROOT);
        if (s.contains("math")) return "Σ";
        if (s.contains("chem")) return "⚗";
        if (s.contains("phys")) return "⚛";
        if (s.contains("hist")) return "🏛";
        if (s.contains("eng")) return "📝";
        if (s.contains("geo")) return "🌐";
        if (s.contains("bio")) return "🧬";
        if (s.contains("comp") || s.contains("it")) return "💻";
        if (s.contains("sci")) return "🔬";
        return "📚";
    }

    /**
     * Convert percentage to letter grade
     * @param percentage Percentage (0-100)
     * @return Letter grade (A+, A, B+, B, C, D, F)
     */
    public static String percentageToGrade(double percentage) {
        if (percentage >= 90) return "A+";
        if (percentage >= 80) return "A";
        if (percentage >= 70) return "B+";
        if (percentage >= 60) return "B";
        if (percentage >= 50) return "C";
        if (percentage >= 40) return "D";
        return "F";
    }
}

