package com.attendx.service;

import com.attendx.dto.*;
import com.attendx.exception.StudentNotFoundException;
import com.attendx.model.Attendance;
import com.attendx.model.Student;
import com.attendx.repository.AttendanceRepository;
import com.attendx.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);
    private final AttendanceRepository attendanceRepo;
    private final StudentRepository studentRepo;

    public AttendanceService(AttendanceRepository attendanceRepo, StudentRepository studentRepo) {
        this.attendanceRepo = attendanceRepo;
        this.studentRepo = studentRepo;
    }

    public void markAttendance(AttendanceRequest req) {
        log.info("Marking attendance for date={}, entries={}", req.date(), req.marks().size());

        // Validate all roll numbers exist
        for (AttendanceRequest.MarkEntry entry : req.marks()) {
            if (!studentRepo.existsByRollNo(entry.rollNo())) {
                throw new IllegalArgumentException(
                        "Student not found with roll number: " + entry.rollNo());
            }
        }

        List<int[]> pairs = req.marks().stream()
                .map(e -> new int[]{e.rollNo(), e.status().charAt(0)})
                .toList();

        attendanceRepo.upsertBatch(req.date(), pairs);
    }

    public DailyReportResponse getDailyReport(LocalDate date) {
        log.debug("Getting daily report for date={}", date);
        List<Student> students = studentRepo.findAll();
        List<Attendance> dayAttendance = attendanceRepo.findByDate(date);

        Map<Integer, Character> statusMap = dayAttendance.stream()
                .collect(Collectors.toMap(Attendance::rollNo, Attendance::status));

        List<DailyReportResponse.StudentStatus> statuses = new ArrayList<>();
        int present = 0, absent = 0, notMarked = 0;

        for (Student s : students) {
            Character status = statusMap.get(s.rollNo());
            String statusStr;
            if (status == null) {
                statusStr = "NOT_MARKED";
                notMarked++;
            } else if (status == 'P') {
                statusStr = "PRESENT";
                present++;
            } else {
                statusStr = "ABSENT";
                absent++;
            }
            statuses.add(new DailyReportResponse.StudentStatus(s.rollNo(), s.name(), statusStr));
        }

        return new DailyReportResponse(date, statuses, present, absent, notMarked);
    }

    public AttendancePercentageResponse getPercentage(int rollNo) {
        log.debug("Getting attendance percentage for rollNo={}", rollNo);
        Student student = studentRepo.findByRollNo(rollNo)
                .orElseThrow(() -> new StudentNotFoundException(rollNo));

        int total = attendanceRepo.countTotalByRollNo(rollNo);
        int presentCount = attendanceRepo.countPresentByRollNo(rollNo);

        if (total == 0) {
            return new AttendancePercentageResponse(
                    rollNo, student.name(), 0, 0, 0.0, "No attendance records");
        }

        double percentage = presentCount * 100.0 / total;
        return new AttendancePercentageResponse(
                rollNo, student.name(), presentCount, total,
                Math.round(percentage * 100.0) / 100.0, null);
    }

    public AttendanceSummaryResponse getSummary() {
        log.debug("Getting attendance summary");
        List<Student> students = studentRepo.findAll();

        List<AttendanceSummaryResponse.StudentSummary> summaries = students.stream()
                .map(s -> {
                    int total = attendanceRepo.countTotalByRollNo(s.rollNo());
                    int present = attendanceRepo.countPresentByRollNo(s.rollNo());
                    double pct = total == 0 ? 0.0 : Math.round(present * 100.0 / total * 100.0) / 100.0;
                    return new AttendanceSummaryResponse.StudentSummary(
                            s.rollNo(), s.name(), present, total, pct);
                })
                .toList();

        double classAverage = summaries.isEmpty() ? 0.0 :
                summaries.stream().mapToDouble(AttendanceSummaryResponse.StudentSummary::percentage)
                        .average().orElse(0.0);
        classAverage = Math.round(classAverage * 100.0) / 100.0;

        List<AttendanceSummaryResponse.DateTrend> trend =
                attendanceRepo.findDistinctDatesWithCounts().stream()
                        .map(d -> new AttendanceSummaryResponse.DateTrend(
                                d.date(), d.present(), d.absent(), d.total()))
                        .toList();

        return new AttendanceSummaryResponse(summaries, classAverage, trend);
    }
}
