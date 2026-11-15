package com.attendx.dto;

public record AttendancePercentageResponse(
    int rollNo,
    String name,
    int present,
    int total,
    double percentage,
    String message
) {}
