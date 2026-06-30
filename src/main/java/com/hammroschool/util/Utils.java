package com.hammroschool.util;

import java.util.Locale;

/**
 * Small collection of helper methods used across multiple controllers.
 *
 * These were previously copy-pasted into every controller.
 * Put them here once and call Utils.initials() / Utils.formatName() everywhere.
 */
public final class Utils {

    // Prevent anyone from creating an instance of this class
    private Utils() {}

    /**
     * Returns up to 2 uppercase initials from a username.
     *
     * Examples:
     *   "sijan"       → "SI"
     *   "ram adhikari" → "RA"
     *   ""            → "?"
     */
    public static String initials(String username) {
        if (username == null || username.isBlank()) return "?";
        String[] parts = username.trim().split("\\s+");
        if (parts.length == 1) {
            // Single word: take first 2 letters
            return parts[0].substring(0, Math.min(2, parts[0].length()))
                           .toUpperCase(Locale.ROOT);
        }
        // Two or more words: one letter from each of the first two words
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1))
                .toUpperCase(Locale.ROOT);
    }

    /**
     * Capitalises the first letter of a username for display.
     *
     * Examples:
     *   "sijan" → "Sijan"
     *   ""      → "Unknown"
     */
    public static String formatName(String username) {
        if (username == null || username.isBlank()) return "Unknown";
        String t = username.trim();
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }
}
