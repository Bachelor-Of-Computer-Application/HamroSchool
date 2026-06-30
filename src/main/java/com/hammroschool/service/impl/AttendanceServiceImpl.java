package com.hammroschool.service.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.hammroschool.config.MongoClientProvider;
import com.hammroschool.model.entity.AttendanceRecord;
import com.hammroschool.service.AttendanceService;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;

public final class AttendanceServiceImpl implements AttendanceService {

    private static final AttendanceServiceImpl INSTANCE = new AttendanceServiceImpl();
    private static final String COL = "attendance";

    private final MongoCollection<Document> col;

    private AttendanceServiceImpl() {
        MongoDatabase db = MongoClientProvider.getInstance().getDatabase();
        this.col = db.getCollection(COL);
        // Unique compound index
        col.createIndex(Indexes.ascending(
                "studentUsername", "teacherUsername", "subjectName", "attendanceDate"));
    }

    public static AttendanceServiceImpl getInstance() { return INSTANCE; }

    // ── Write ─────────────────────────────────────────────────────────────────

    @Override
    public synchronized void saveAttendance(String studentUsername, String teacherUsername,
                                             String subjectName, LocalDate date, String status) {
        Date mongoDate = toMongoDate(date);
        org.bson.conversions.Bson filter = Filters.and(
                Filters.eq("studentUsername", studentUsername),
                Filters.eq("teacherUsername", teacherUsername),
                Filters.eq("subjectName",     subjectName),
                Filters.eq("attendanceDate",  mongoDate));

        Document doc = new Document("studentUsername", studentUsername)
                .append("teacherUsername", teacherUsername)
                .append("subjectName",     subjectName)
                .append("attendanceDate",  mongoDate)
                .append("status",          status == null ? "PRESENT" : status.toUpperCase());

        col.replaceOne(filter, doc, new ReplaceOptions().upsert(true));
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    public synchronized List<AttendanceRecord> getAttendanceForDate(String teacherUsername,
                                                                      String subjectName,
                                                                      LocalDate date) {
        Date mongoDate = toMongoDate(date);
        List<AttendanceRecord> result = new ArrayList<>();
        for (Document d : col.find(Filters.and(
                Filters.eq("teacherUsername", teacherUsername),
                Filters.eq("subjectName",     subjectName),
                Filters.eq("attendanceDate",  mongoDate)))
                .sort(Sorts.ascending("studentUsername"))) {
            result.add(mapDoc(d));
        }
        return result;
    }

    @Override
    public synchronized Map<String, Double> getAttendancePercentages(String teacherUsername,
                                                                       String subjectName) {
        Map<String, int[]> totals = new HashMap<>();  // username → [attended, total]
        for (Document d : col.find(Filters.and(
                Filters.eq("teacherUsername", teacherUsername),
                Filters.eq("subjectName",     subjectName)))) {
            String student = d.getString("studentUsername");
            String status  = d.getString("status");
            int[] arr = totals.computeIfAbsent(student, k -> new int[]{0, 0});
            arr[1]++;  // total
            if ("PRESENT".equals(status) || "LATE".equals(status)) arr[0]++;  // attended
        }
        Map<String, Double> result = new HashMap<>();
        totals.forEach((student, arr) -> {
            double pct = arr[1] > 0 ? Math.round(arr[0] * 1000.0 / arr[1]) / 10.0 : 0.0;
            result.put(student, pct);
        });
        return result;
    }

    @Override
    public synchronized String getStatusForToday(String studentUsername, String teacherUsername,
                                                   String subjectName, LocalDate date) {
        Document doc = col.find(Filters.and(
                Filters.eq("studentUsername", studentUsername),
                Filters.eq("teacherUsername", teacherUsername),
                Filters.eq("subjectName",     subjectName),
                Filters.eq("attendanceDate",  toMongoDate(date)))).first();
        return doc != null ? doc.getString("status") : "PRESENT";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AttendanceRecord mapDoc(Document d) {
        ObjectId oid = d.getObjectId("_id");
        long numId   = oid != null ? oid.getTimestamp() : -1;

        Date mongoDate = d.getDate("attendanceDate");
        LocalDate date = mongoDate != null
                ? mongoDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                : LocalDate.now();

        return new AttendanceRecord(numId,
                d.getString("studentUsername"),
                d.getString("teacherUsername"),
                d.getString("subjectName"),
                date,
                d.getString("status"));
    }

    /** Convert LocalDate to java.util.Date (midnight UTC) for MongoDB storage. */
    private static Date toMongoDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.of("UTC")).toInstant());
    }
}
