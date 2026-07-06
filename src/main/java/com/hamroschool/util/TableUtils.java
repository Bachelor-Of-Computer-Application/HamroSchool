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
     * Create a formatted count label (e.g., "5 students", "1 teacher")
     * @param count The count
     * @param singular Singular form (e.g., "student")
     * @param plural Plural form (e.g., "students")
     * @return Formatted string
     */
    public static String formatCount(int count, String singular, String plural) {
        return count + " " + (count == 1 ? singular : plural);
    }

    /**
     * Create a formatted count label using simple pluralization (add 's')
     * @param count The count
     * @param noun Base noun (e.g., "student")
     * @return Formatted string (e.g., "5 students", "1 student")
     */
    public static String formatCount(int count, String noun) {
        return formatCount(count, noun, noun + "s");
    }

    /**
     * Create a summary label text for filtered results
     * @param filteredCount Number of filtered items
     * @param totalCount Total number of items
     * @param itemName Name of item type (e.g., "accounts", "classes")
     * @return Summary text (e.g., "Showing 5 of 10 accounts")
     */
    public static String createSummary(int filteredCount, int totalCount, String itemName) {
        return "Showing " + filteredCount + " of " + totalCount + " " + itemName;
    }

    /**
     * Create a summary label text for paginated results
     * @param from Start index (1-based)
     * @param to End index (inclusive)
     * @param total Total number of items
     * @param itemName Name of item type (e.g., "accounts", "classes")
     * @return Summary text (e.g., "Showing 1–10 of 50 accounts")
     */
    public static String createPaginatedSummary(int from, int to, int total, String itemName) {
        if (total == 0) {
            return "Showing 0 of 0 " + itemName;
        }
        return "Showing " + from + "–" + to + " of " + total + " " + itemName;
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

    /**
     * Check if grade is passing
     * @param percentage Percentage (0-100)
     * @return true if >= 40%
     */
    public static boolean isPassing(double percentage) {
        return percentage >= 40;
    }

    /**
     * Format a date string to show only date portion (first 10 chars)
     * @param dateTimeString Full date-time string
     * @return Date portion or "—" if null/empty
     */
    public static String formatDateOnly(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isBlank()) {
            return "—";
        }
        return dateTimeString.substring(0, Math.min(10, dateTimeString.length()));
    }
}
