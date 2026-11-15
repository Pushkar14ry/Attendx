package com.attendx.dto;

import java.time.LocalDate;
import java.util.List;

public record DailyReportResponse(
    LocalDate date,
    List<StudentStatus> students,
    int present,
    int absent,
    int notMarked
) {
    public record StudentStatus(
        int rollNo,
        String name,
        String status  // "PRESENT", "ABSENT", "NOT_MARKED"
    ) {}
}
