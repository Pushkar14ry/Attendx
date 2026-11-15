package com.attendx.ui.model;

import java.time.LocalDate;
import java.util.List;

public record DailyReport(
    LocalDate date,
    List<StudentStatus> students,
    int present,
    int absent,
    int notMarked
) {
    public record StudentStatus(int rollNo, String name, String status) {
        public int getRollNo() { return rollNo; }
        public String getName() { return name; }
        public String getStatus() { return status; }
    }
}
