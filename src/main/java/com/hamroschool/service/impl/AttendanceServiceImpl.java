package com.hamroschool.service.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.hamroschool.config.MongoClientProvider;
import com.hamroschool.model.entity.AttendanceRecord;
import com.hamroschool.service.AttendanceService;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
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
        col.createIndex(Indexes.ascending(
                "studentUsername", "teacherUsername", "subjectName", "attendanceDate"));
    }

    public static AttendanceServiceImpl getInstance() { return INSTANCE; }


    @Override
    public void saveAttendance(String studentUsername, String teacherUsername,
                               String subjectName, LocalDate date, String status,
                               String feedback) {
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
            .append("status",          status == null ? "PRESENT" : status.toUpperCase())
            .append("feedback",        feedback == null ? "" : feedback.trim());

        col.replaceOne(filter, doc, new ReplaceOptions().upsert(true));
    }


    @Override
    public List<AttendanceRecord> getAttendanceForDate(String teacherUsername,
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
    public Map<String, Double> getAttendancePercentages(String teacherUsername,
                                                        String subjectName) {
        Map<String, int[]> totals = getAttendanceTotals(teacherUsername, subjectName);
        Map<String, Double> result = new HashMap<>();
        totals.forEach((student, arr) -> {
            double pct = arr[1] > 0 ? Math.round(arr[0] * 1000.0 / arr[1]) / 10.0 : 0.0;
            result.put(student, pct);
        });
        return result;
    }

    @Override
    public Map<String, int[]> getAttendanceTotals(String teacherUsername,
                                                   String subjectName) {
        // Use a server-side $group aggregation to avoid transferring all records to the client.
        Map<String, int[]> totals = new HashMap<>();
        col.aggregate(Arrays.asList(
                Aggregates.match(Filters.and(
                        Filters.eq("teacherUsername", teacherUsername),
                        Filters.eq("subjectName",     subjectName))),
                Aggregates.group(
                        "$studentUsername",
                        Accumulators.sum("total", 1),
                        Accumulators.sum("attended",
                                new org.bson.Document("$cond", Arrays.asList(
                                        new org.bson.Document("$in", Arrays.asList(
                                                "$status", Arrays.asList("PRESENT", "LATE"))),
                                        1, 0))))
        )).forEach(d -> {
            String student = d.getString("_id");
            int total    = d.getInteger("total",    0);
            int attended = d.getInteger("attended", 0);
            totals.put(student, new int[]{attended, total});
        });
        return totals;
    }

    @Override
    public String getStatusForToday(String studentUsername, String teacherUsername,
                                    String subjectName, LocalDate date) {
        Document doc = col.find(Filters.and(
                Filters.eq("studentUsername", studentUsername),
                Filters.eq("teacherUsername", teacherUsername),
                Filters.eq("subjectName",     subjectName),
                Filters.eq("attendanceDate",  toMongoDate(date)))).first();
        return doc != null ? doc.getString("status") : "PRESENT";
    }


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
            d.getString("status"),
            d.getString("feedback"));
    }

    private static Date toMongoDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.of("UTC")).toInstant());
    }
}
