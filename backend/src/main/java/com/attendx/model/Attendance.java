package com.attendx.model;

import java.time.LocalDate;

public record Attendance(
    Long id,
    Integer rollNo,
    LocalDate classDate,
    char status
) {}
