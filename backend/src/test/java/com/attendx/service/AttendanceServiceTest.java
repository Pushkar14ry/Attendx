package com.attendx.service;

import com.attendx.dto.AttendancePercentageResponse;
import com.attendx.dto.DailyReportResponse;
import com.attendx.exception.StudentNotFoundException;
import com.attendx.model.Attendance;
import com.attendx.model.Student;
import com.attendx.repository.AttendanceRepository;
import com.attendx.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock AttendanceRepository attendanceRepo;
    @Mock StudentRepository studentRepo;
    @InjectMocks AttendanceService service;

    private Student student(int rollNo, String name) {
        return new Student(rollNo, name, LocalDateTime.now());
    }

    @Test
    void percentage_calculatesCorrectly() {
        when(studentRepo.findByRollNo(1)).thenReturn(Optional.of(student(1, "Nihan")));
        when(attendanceRepo.countTotalByRollNo(1)).thenReturn(10);
        when(attendanceRepo.countPresentByRollNo(1)).thenReturn(8);

        AttendancePercentageResponse resp = service.getPercentage(1);

        assertThat(resp.percentage()).isEqualTo(80.0);
        assertThat(resp.present()).isEqualTo(8);
        assertThat(resp.total()).isEqualTo(10);
        assertThat(resp.message()).isNull();
    }

    @Test
    void percentage_returnsZeroWhenNoRecords() {
        when(studentRepo.findByRollNo(1)).thenReturn(Optional.of(student(1, "Nihan")));
        when(attendanceRepo.countTotalByRollNo(1)).thenReturn(0);
        when(attendanceRepo.countPresentByRollNo(1)).thenReturn(0);

        AttendancePercentageResponse resp = service.getPercentage(1);

        assertThat(resp.percentage()).isEqualTo(0.0);
        assertThat(resp.message()).isEqualTo("No attendance records");
    }

    @Test
    void percentage_throwsWhenStudentNotFound() {
        when(studentRepo.findByRollNo(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getPercentage(99))
                .isInstanceOf(StudentNotFoundException.class);
    }

    @Test
    void dailyReport_includesNotMarkedStudents() {
        LocalDate date = LocalDate.of(2026, 11, 5);
        when(studentRepo.findAll()).thenReturn(List.of(
                student(1, "Nihan"),
                student(2, "Asha")
        ));
        when(attendanceRepo.findByDate(date)).thenReturn(List.of(
                new Attendance(1L, 1, date, 'P')
                // Asha has no attendance -> NOT_MARKED
        ));

        DailyReportResponse report = service.getDailyReport(date);

        assertThat(report.present()).isEqualTo(1);
        assertThat(report.absent()).isEqualTo(0);
        assertThat(report.notMarked()).isEqualTo(1);
        assertThat(report.students()).extracting(DailyReportResponse.StudentStatus::status)
                .containsExactly("PRESENT", "NOT_MARKED");
    }

    @Test
    void dailyReport_handlesUpsertOverwrite() {
        LocalDate date = LocalDate.of(2026, 11, 5);
        when(studentRepo.findAll()).thenReturn(List.of(student(1, "Nihan")));
        when(attendanceRepo.findByDate(date)).thenReturn(List.of(
                new Attendance(1L, 1, date, 'A')  // was P, now A after upsert
        ));

        DailyReportResponse report = service.getDailyReport(date);

        assertThat(report.absent()).isEqualTo(1);
        assertThat(report.present()).isEqualTo(0);
    }
}
