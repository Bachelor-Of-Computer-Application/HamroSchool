package com.hamroschool.model.dto;

/**
 * Aggregated per-student mark summary for the marksheet view.
 * Midterm + Final are pulled from the marks table by exam_type.
 */
public class StudentMarkSummary {
    private final int roll;
    private final String username;
    private final int midterm;   // -1 = not entered
    private final int finalMark; // -1 = not entered
    private final int fullMarks; // total full marks (midterm FM + final FM)

    public StudentMarkSummary(int roll, String username, int midterm, int finalMark, int fullMarks) {
        this.roll      = roll;
        this.username  = username;
        this.midterm   = midterm;
        this.finalMark = finalMark;
        this.fullMarks = fullMarks;
    }

    public int    getRoll()      { return roll; }
    public String getUsername()  { return username; }
    public int    getMidterm()   { return midterm; }
    public int    getFinalMark() { return finalMark; }

    public int getTotal() {
        int m = Math.max(midterm, 0);
        int f = Math.max(finalMark, 0);
        return m + f;
    }

    public int getFullMarks() { return fullMarks > 0 ? fullMarks : 100; }

    public double getPercentage() {
        int fm = getFullMarks();
        return fm > 0 ? Math.round(getTotal() * 1000.0 / fm) / 10.0 : 0;
    }

    /** Pass / Average / Needs Support based on percentage */
    public String getStatus() {
        double pct = getPercentage();
        if (pct >= 60) return "Pass";
        if (pct >= 40) return "Average";
        return "Needs Support";
    }
}
