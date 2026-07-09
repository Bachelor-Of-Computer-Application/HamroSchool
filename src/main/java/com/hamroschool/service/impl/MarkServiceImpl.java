package com.hamroschool.service.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.hamroschool.config.MongoClientProvider;
import com.hamroschool.model.auth.UserRole;
import com.hamroschool.model.dto.ReportCardEntry;
import com.hamroschool.model.dto.StudentMarkSummary;
import com.hamroschool.model.entity.Mark;
import com.hamroschool.service.MarkService;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;

public final class MarkServiceImpl implements MarkService {

    private static final MarkServiceImpl INSTANCE = new MarkServiceImpl();

    private static final String COL_MARKS    = "marks";
    private static final String COL_ACCOUNTS = "user_accounts";

    private final MongoCollection<Document> marks;
    private final MongoCollection<Document> userAccounts;

    private MarkServiceImpl() {
        MongoDatabase db = MongoClientProvider.getInstance().getDatabase();
        this.marks        = db.getCollection(COL_MARKS);
        this.userAccounts = db.getCollection(COL_ACCOUNTS);

        marks.createIndex(Indexes.ascending(
                "studentUsername", "subjectName", "teacherUsername", "examType"));
    }

    public static MarkServiceImpl getInstance() { return INSTANCE; }


    @Override
    public synchronized long saveMark(String studentUsername, String subjectName,
                                      String teacherUsername, int score, int fullMarks,
                                      String examType, String remarks) {
        String et = examType == null ? "Terminal" : examType;
        org.bson.conversions.Bson filter = Filters.and(
                Filters.eq("studentUsername", studentUsername),
                Filters.eq("subjectName",     subjectName),
                Filters.eq("teacherUsername", teacherUsername),
                Filters.eq("examType",        et));

        Document doc = new Document("studentUsername", studentUsername)
                .append("subjectName",     subjectName)
                .append("teacherUsername", teacherUsername)
                .append("score",           score)
                .append("fullMarks",       fullMarks)
                .append("examType",        et)
                .append("remarks",         remarks == null ? "" : remarks)
                .append("createdAt",       Instant.now());

        marks.replaceOne(filter, doc, new ReplaceOptions().upsert(true));
        Document inserted = marks.find(filter).first();
        if (inserted != null && inserted.getObjectId("_id") != null) {
            return inserted.getObjectId("_id").getTimestamp();
        }
        return -1;
    }

    @Override
    public synchronized List<Mark> getMarksByTeacher(String teacherUsername) {
        List<Mark> list = new ArrayList<>();
        for (Document d : marks.find(Filters.eq("teacherUsername", teacherUsername))
                .sort(Sorts.descending("createdAt"))) {
            list.add(mapMark(d));
        }
        return list;
    }

    @Override
    public synchronized List<Mark> getMarksByStudent(String studentUsername) {
        List<Mark> list = new ArrayList<>();
        for (Document d : marks.find(Filters.eq("studentUsername", studentUsername))
                .sort(Sorts.ascending("subjectName"))) {
            list.add(mapMark(d));
        }
        return list;
    }

    @Override
    public synchronized List<String> getSubjectsByTeacher(String teacherUsername) {
        return marks.distinct("subjectName", Filters.eq("teacherUsername", teacherUsername),
                String.class).into(new ArrayList<>());
    }

    @Override
    public synchronized List<String> getAllStudentUsernames() {
        return userAccounts.distinct("username",
                Filters.eq("role", UserRole.STUDENT.name()),
                String.class).into(new ArrayList<>());
    }


    @Override
    public synchronized List<StudentMarkSummary> getMarksheet(String teacherUsername,
                                                               String subjectName) {
        Map<String, int[]> pivot = new LinkedHashMap<>();
        for (Document d : marks.find(Filters.and(
                Filters.eq("teacherUsername", teacherUsername),
                Filters.eq("subjectName",     subjectName)))
                .sort(Sorts.ascending("studentUsername"))) {
            String student  = d.getString("studentUsername");
            String examType = d.getString("examType").toLowerCase();
            int score       = d.getInteger("score", 0);
            int fullMarks   = d.getInteger("fullMarks", 50);
            int[] row = pivot.computeIfAbsent(student, k -> new int[]{-1, -1, 50, 50});
            if (examType.contains("mid")) { row[0] = score; row[2] = fullMarks; }
            else                          { row[1] = score; row[3] = fullMarks; }
        }
        List<String> allStudents = getAllStudentUsernames();
        List<StudentMarkSummary> result = new ArrayList<>();
        int roll = 1;
        for (String student : allStudents) {
            int[] row = pivot.getOrDefault(student, new int[]{-1, -1, 50, 50});
            int fm = (row[2] > 0 ? row[2] : 50) + (row[3] > 0 ? row[3] : 50);
            result.add(new StudentMarkSummary(roll++, student, row[0], row[1], fm));
        }
        return result;
    }

    @Override
    public synchronized double getAverageMarks(String teacherUsername, String subjectName) {
        return getMarksheet(teacherUsername, subjectName).stream()
                .filter(s -> s.getMidterm() >= 0 || s.getFinalMark() >= 0)
                .mapToDouble(StudentMarkSummary::getPercentage).average().orElse(0);
    }

    @Override
    public synchronized double getPassRate(String teacherUsername, String subjectName) {
        List<StudentMarkSummary> with = getMarksheet(teacherUsername, subjectName).stream()
                .filter(s -> s.getMidterm() >= 0 || s.getFinalMark() >= 0).toList();
        if (with.isEmpty()) return 0;
        long passed = with.stream().filter(s -> s.getPercentage() >= 40).count();
        return Math.round(passed * 1000.0 / with.size()) / 10.0;
    }

    @Override
    public synchronized int getTopScore(String teacherUsername, String subjectName) {
        return getMarksheet(teacherUsername, subjectName).stream()
                .filter(s -> s.getMidterm() >= 0 || s.getFinalMark() >= 0)
                .mapToInt(StudentMarkSummary::getTotal).max().orElse(0);
    }

    @Override
    public synchronized List<ReportCardEntry> getReportCard(String teacherUsername,
                                                             String subjectName) {
        List<StudentMarkSummary> sheet = getMarksheet(teacherUsername, subjectName);
        List<StudentMarkSummary> sorted = sheet.stream()
                .sorted((a, b) -> Double.compare(b.getPercentage(), a.getPercentage())).toList();
        Map<String, Integer> rankMap = new LinkedHashMap<>();
        int rank = 1;
        for (int i = 0; i < sorted.size(); i++) {
            StudentMarkSummary s = sorted.get(i);
            if (i > 0 && sorted.get(i - 1).getPercentage() == s.getPercentage()) {
                rankMap.put(s.getUsername(), rankMap.get(sorted.get(i - 1).getUsername()));
            } else {
                rankMap.put(s.getUsername(), rank);
            }
            rank++;
        }
        List<ReportCardEntry> result = new ArrayList<>();
        for (StudentMarkSummary s : sheet) {
            result.add(new ReportCardEntry(s.getRoll(), s.getUsername(),
                    s.getPercentage(), rankMap.getOrDefault(s.getUsername(), sheet.size())));
        }
        return result;
    }




    private Mark mapMark(Document d) {
        ObjectId oid = d.getObjectId("_id");
        long numId   = oid != null ? oid.getTimestamp() : -1;

        LocalDateTime createdAt;
        Object raw = d.get("createdAt");
        if (raw instanceof java.util.Date date) {
            createdAt = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        } else if (raw instanceof Instant inst) {
            createdAt = inst.atZone(ZoneId.systemDefault()).toLocalDateTime();
        } else {
            createdAt = LocalDateTime.now();
        }

        return new Mark(numId,
                d.getString("studentUsername"),
                d.getString("subjectName"),
                d.getString("teacherUsername"),
                d.getInteger("score",     0),
                d.getInteger("fullMarks", 100),
                d.getString("examType"),
                d.getString("remarks"),
                createdAt);
    }

    private boolean blank(String s) { return s == null || s.trim().isEmpty(); }
}
