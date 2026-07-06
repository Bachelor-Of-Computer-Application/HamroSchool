package com.hamroschool.util;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public final class FilterUtils {
    
    private FilterUtils() {} // Prevent instantiation

    /**
     * Filter a list based on text query matching any of the provided fields
     * @param <T> The type of items in the list
     * @param items The list to filter
     * @param query The search query
     * @param fieldExtractors Functions to extract searchable text from each item
     * @return Filtered list
     */
    @SafeVarargs
    public static <T> List<T> filterByText(List<T> items, String query, 
                                           Function<T, String>... fieldExtractors) {
        String normalizedQuery = normalizeQuery(query);
        
        if (normalizedQuery.isEmpty()) {
            return items;
        }
        
        return items.stream()
            .filter(item -> matchesAnyField(item, normalizedQuery, fieldExtractors))
            .toList();
    }

    /**
     * Filter a list of strings by text query
     * @param items The list to filter
     * @param query The search query
     * @return Filtered list
     */
    public static List<String> filterStrings(List<String> items, String query) {
        String normalizedQuery = normalizeQuery(query);
        
        if (normalizedQuery.isEmpty()) {
            return items;
        }
        
        return items.stream()
            .filter(item -> item != null && 
                    item.toLowerCase(Locale.ROOT).contains(normalizedQuery))
            .toList();
    }

    public static String normalizeQuery(String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }
    @SafeVarargs
    private static <T> boolean matchesAnyField(T item, String normalizedQuery, 
                                               Function<T, String>... fieldExtractors) {
        for (Function<T, String> extractor : fieldExtractors) {
            try {
                String fieldValue = extractor.apply(item);
                if (fieldValue != null && 
                    fieldValue.toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
                    return true;
                }
            } catch (Exception e) {
            }
        }
        return false;
    }

    public static <T> Function<String, List<T>> createUsernameFilter(
            List<T> allItems, Function<T, String> usernameExtractor) {
        
        return query -> filterByText(allItems, query, usernameExtractor);
    }

    public static <T> Function<String, List<T>> createDualFieldFilter(
            List<T> allItems, 
            Function<T, String> field1Extractor,
            Function<T, String> field2Extractor) {
        
        return query -> filterByText(allItems, query, field1Extractor, field2Extractor);
    }

    public static boolean matches(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return false;
        }
        return str1.trim().equalsIgnoreCase(str2.trim());
    }

    public static boolean contains(String haystack, String needle) {
        if (haystack == null || needle == null) {
            return false;
        }
        return haystack.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }
}
