package com.attendx.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;

public record AttendanceRequest(
    @NotNull(message = "Date is required")
    LocalDate date,

    @NotEmpty(message = "Marks list must not be empty")
    @Valid
    List<MarkEntry> marks
) {
    public record MarkEntry(
        @Positive(message = "Roll number must be positive")
        @NotNull(message = "Roll number is required")
        Integer rollNo,

        @NotNull(message = "Status is required")
        @Pattern(regexp = "[PA]", message = "Status must be P (Present) or A (Absent)")
        String status
    ) {}
}
