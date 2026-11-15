package com.attendx.ui.service;

import com.attendx.ui.model.*;
import com.fasterxml.jackson.core.type.TypeReference;

import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.*;

public class AttendXService {

    private final ApiClient client;

    public AttendXService(ApiClient client) {
        this.client = client;
    }

    // ── Students ────────────────────────────────────────────────
    public List<Student> listStudents() throws Exception {
        HttpResponse<String> resp = client.get("/students");
        checkStatus(resp, 200);
        return client.parse(resp.body(), new TypeReference<>() {});
    }

    public Student addStudent(int rollNo, String name) throws Exception {
        Map<String, Object> body = Map.of("rollNo", rollNo, "name", name);
        HttpResponse<String> resp = client.post("/students", body);
        if (resp.statusCode() == 409) {
            throw new DuplicateRollException("Roll number " + rollNo + " already exists");
        }
        checkStatus(resp, 201);
        return client.parse(resp.body(), Student.class);
    }

    public void deleteStudent(int rollNo) throws Exception {
        HttpResponse<String> resp = client.delete("/students/" + rollNo);
        checkStatus(resp, 204);
    }

    // ── Attendance ───────────────────────────────────────────────
    public void markAttendance(LocalDate date, List<Map<String, Object>> marks) throws Exception {
        Map<String, Object> body = Map.of("date", date.toString(), "marks", marks);
        HttpResponse<String> resp = client.post("/attendance", body);
        checkStatus(resp, 200);
    }

    public DailyReport getDailyReport(LocalDate date) throws Exception {
        HttpResponse<String> resp = client.get("/attendance/daily?date=" + date);
        checkStatus(resp, 200);
        return client.parse(resp.body(), DailyReport.class);
    }

    public AttendancePercentage getPercentage(int rollNo) throws Exception {
        HttpResponse<String> resp = client.get("/attendance/percentage/" + rollNo);
        checkStatus(resp, 200);
        return client.parse(resp.body(), AttendancePercentage.class);
    }

    public AttendanceSummary getSummary() throws Exception {
        HttpResponse<String> resp = client.get("/attendance/summary");
        checkStatus(resp, 200);
        return client.parse(resp.body(), AttendanceSummary.class);
    }

    // ── Health ───────────────────────────────────────────────────
    public boolean isBackendOnline() {
        try {
            HttpResponse<String> resp = client.get("/../actuator/health");
            return resp.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private void checkStatus(HttpResponse<String> resp, int expected) throws Exception {
        if (resp.statusCode() != expected) {
            throw new ApiException("API error " + resp.statusCode() + ": " + resp.body());
        }
    }

    public static class DuplicateRollException extends RuntimeException {
        public DuplicateRollException(String msg) { super(msg); }
    }

    public static class ApiException extends Exception {
        public ApiException(String msg) { super(msg); }
    }
}
