package com.hammroschool.model.dto;

/**
 * One row in the report card table — per-student grade, GPA, rank, status.
 */
public class ReportCardEntry {
    private final int    roll;
    private final String username;
    private final double percentage;
    private final int    rank;

    public ReportCardEntry(int roll, String username, double percentage, int rank) {
        this.roll       = roll;
        this.username   = username;
        this.percentage = percentage;
        this.rank       = rank;
    }

    public int    getRoll()       { return roll; }
    public String getUsername()   { return username; }
    public double getPercentage() { return percentage; }
    public int    getRank()       { return rank; }

    /** Letter grade with +/- based on percentage. */
    public String getGrade() {
        if (percentage >= 93) return "A";
        if (percentage >= 90) return "A-";
        if (percentage >= 87) return "B+";
        if (percentage >= 83) return "B";
        if (percentage >= 80) return "B-";
        if (percentage >= 77) return "C+";
        if (percentage >= 73) return "C";
        if (percentage >= 70) return "C-";
        if (percentage >= 67) return "D+";
        if (percentage >= 60) return "D";
        return "F";
    }

    /** GPA on a 4.0 scale. */
    public double getGpa() {
        if (percentage >= 93) return 4.0;
        if (percentage >= 90) return 3.7;
        if (percentage >= 87) return 3.3;
        if (percentage >= 83) return 3.0;
        if (percentage >= 80) return 2.7;
        if (percentage >= 77) return 2.3;
        if (percentage >= 73) return 2.0;
        if (percentage >= 70) return 1.7;
        if (percentage >= 67) return 1.3;
        if (percentage >= 60) return 1.0;
        return 0.0;
    }

    /** Excellent / Good / Average / Needs Support */
    public String getStatus() {
        if (percentage >= 80) return "Excellent";
        if (percentage >= 60) return "Good";
        if (percentage >= 40) return "Average";
        return "Needs Support";
    }
}
