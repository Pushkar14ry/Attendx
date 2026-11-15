package com.attendx.controller;

import com.attendx.dto.*;
import com.attendx.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/attendance")
@Tag(name = "Attendance", description = "Attendance management endpoints")
public class AttendanceController {

    private final AttendanceService service;

    public AttendanceController(AttendanceService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Mark attendance for a date (upsert)")
    public ResponseEntity<Void> markAttendance(@Valid @RequestBody AttendanceRequest req) {
        service.markAttendance(req);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/daily")
    @Operation(summary = "Get daily attendance report")
    public DailyReportResponse getDailyReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.getDailyReport(date);
    }

    @GetMapping("/percentage/{rollNo}")
    @Operation(summary = "Get attendance percentage for a student")
    public AttendancePercentageResponse getPercentage(@PathVariable("rollNo") int rollNo) {
        return service.getPercentage(rollNo);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get overall attendance summary with trends")
    public AttendanceSummaryResponse getSummary() {
        return service.getSummary();
    }
}
