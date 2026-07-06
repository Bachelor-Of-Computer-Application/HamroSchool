package com.hamroschool.util;

public final class UIStyles {
    
    private UIStyles() {} 
    
    
    public static final String NAV_BUTTON_ACTIVE = 
        "-fx-background-color: #111111; " +
        "-fx-background-radius: 8; " +
        "-fx-text-fill: white; " +
        "-fx-font-size: 13px; " +
        "-fx-font-weight: 600; " +
        "-fx-padding: 0 12 0 12; " +
        "-fx-cursor: hand;";
    
    public static final String NAV_BUTTON_INACTIVE = 
        "-fx-background-color: transparent; " +
        "-fx-background-radius: 8; " +
        "-fx-text-fill: #44403c; " +
        "-fx-font-size: 13px; " +
        "-fx-font-weight: 500; " +
        "-fx-padding: 0 12 0 12; " +
        "-fx-cursor: hand;";
    
    
    public static final String AVATAR_DEFAULT = 
        "-fx-background-color: #e8e8e6; " +
        "-fx-text-fill: #444444; " +
        "-fx-font-size: 11px; " +
        "-fx-font-weight: 800; " +
        "-fx-background-radius: 999; " +
        "-fx-min-width: 30; " +
        "-fx-min-height: 30; " +
        "-fx-pref-width: 30; " +
        "-fx-pref-height: 30; " +
        "-fx-alignment: center;";
    
    public static final String AVATAR_ADMIN = 
        "-fx-background-color: #111111; " +
        "-fx-text-fill: white; " +
        "-fx-font-size: 11px; " +
        "-fx-font-weight: 800; " +
        "-fx-background-radius: 999; " +
        "-fx-min-width: 30; " +
        "-fx-min-height: 30; " +
        "-fx-pref-width: 30; " +
        "-fx-pref-height: 30; " +
        "-fx-alignment: center;";
    
    public static final String AVATAR_SMALL = 
        "-fx-background-color: #e8e8e6; " +
        "-fx-text-fill: #444444; " +
        "-fx-font-size: 11px; " +
        "-fx-font-weight: 800; " +
        "-fx-background-radius: 999; " +
        "-fx-min-width: 28; " +
        "-fx-min-height: 28; " +
        "-fx-pref-width: 28; " +
        "-fx-pref-height: 28; " +
        "-fx-alignment: center;";
    
    
    public static final String BADGE_DEFAULT = 
        "-fx-background-color: #f4f4f5; " +
        "-fx-text-fill: #111111; " +
        "-fx-padding: 5 10 5 10; " +
        "-fx-background-radius: 999; " +
        "-fx-font-size: 11px; " +
        "-fx-font-weight: 700;";
    
    public static final String BADGE_NEUTRAL = 
        "-fx-background-color: #f5f5f4; " +
        "-fx-border-color: #e7e5e4; " +
        "-fx-border-radius: 6; " +
        "-fx-background-radius: 6; " +
        "-fx-text-fill: #111111; " +
        "-fx-font-size: 12px; " +
        "-fx-font-weight: 700; " +
        "-fx-padding: 3 10 3 10;";
    
    
    public static final String PRIMARY_BUTTON = 
        "-fx-background-color: #111111; " +
        "-fx-text-fill: white; " +
        "-fx-font-size: 12px; " +
        "-fx-font-weight: 600; " +
        "-fx-padding: 6 14; " +
        "-fx-background-radius: 6; " +
        "-fx-cursor: hand;";
    
    public static final String DANGER_BUTTON = 
        "-fx-background-color: #dc2626; " +
        "-fx-text-fill: white; " +
        "-fx-font-size: 11px; " +
        "-fx-font-weight: 600; " +
        "-fx-padding: 4 12; " +
        "-fx-background-radius: 4; " +
        "-fx-cursor: hand;";
    
    public static final String WARNING_BUTTON = 
        "-fx-background-color: #f97316; " +
        "-fx-text-fill: white; " +
        "-fx-font-size: 12px; " +
        "-fx-font-weight: 600; " +
        "-fx-padding: 6 14; " +
        "-fx-background-radius: 6; " +
        "-fx-cursor: hand;";
    
    public static final String ICON_BUTTON = 
        "-fx-background-color: transparent; " +
        "-fx-cursor: hand; " +
        "-fx-padding: 6 8 6 8; " +
        "-fx-background-radius: 999;";
    
    
    public static final String TEXT_BOLD_PRIMARY = 
        "-fx-text-fill: #111111; " +
        "-fx-font-size: 13px; " +
        "-fx-font-weight: 700;";
    
    public static final String TEXT_NORMAL_SECONDARY = 
        "-fx-text-fill: #44403c; " +
        "-fx-font-size: 13px; " +
        "-fx-font-weight: 400;";
    
    public static final String TEXT_BOLD_SECONDARY = 
        "-fx-text-fill: #222222; " +
        "-fx-font-size: 13px; " +
        "-fx-font-weight: 700;";
    
    public static final String TEXT_TERTIARY = 
        "-fx-text-fill: #78716c; " +
        "-fx-font-size: 13px; " +
        "-fx-font-weight: 400;";
    
    
    public static final String COLOR_PRIMARY = "#111111";
    public static final String COLOR_SUCCESS = "#16a34a";
    public static final String COLOR_WARNING = "#d97706";
    public static final String COLOR_DANGER = "#dc2626";
    public static final String COLOR_INFO = "#2563eb";
    
    
    /**
     * Get color based on percentage (for attendance, grades, etc.)
     * @param percentage Value from 0-100
     * @return Color hex code
     */
    public static String getPercentageColor(double percentage) {
        if (percentage >= 75) return COLOR_SUCCESS;  // Green
        if (percentage >= 50) return COLOR_WARNING;  // Orange
        return COLOR_DANGER;                          // Red
    }
    
    public static String createBadgeStyle(String bgColor, String textColor) {
        return String.format(
            "-fx-background-color: %s; " +
            "-fx-text-fill: %s; " +
            "-fx-padding: 4 12 4 12; " +
            "-fx-background-radius: 999; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 700;",
            bgColor, textColor
        );
    }
}
