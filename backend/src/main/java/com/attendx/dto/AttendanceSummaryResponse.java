package com.attendx.dto;

import java.time.LocalDate;
import java.util.List;

public record AttendanceSummaryResponse(
    List<StudentSummary> students,
    double classAverage,
    List<DateTrend> trend
) {
    public record StudentSummary(
        int rollNo,
        String name,
        int present,
        int total,
        double percentage
    ) {}

    public record DateTrend(
        LocalDate date,
        int present,
        int absent,
        int total
    ) {}
}
