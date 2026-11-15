package com.attendx.ui.model;

public record AttendancePercentage(
    int rollNo,
    String name,
    int present,
    int total,
    double percentage,
    String message
) {}
