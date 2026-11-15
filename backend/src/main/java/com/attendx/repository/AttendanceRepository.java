package com.attendx.repository;

import com.attendx.model.Attendance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public class AttendanceRepository {

    private static final Logger log = LoggerFactory.getLogger(AttendanceRepository.class);
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    public AttendanceRepository(JdbcTemplate jdbc, NamedParameterJdbcTemplate namedJdbc) {
        this.jdbc = jdbc;
        this.namedJdbc = namedJdbc;
    }

    private static final RowMapper<Attendance> ROW_MAPPER = (rs, rowNum) -> new Attendance(
            rs.getLong("id"),
            rs.getInt("roll_no"),
            rs.getDate("class_date").toLocalDate(),
            rs.getString("status").charAt(0)
    );

    public List<Attendance> findByDate(LocalDate date) {
        return jdbc.query(
                "SELECT id, roll_no, class_date, status FROM attendance WHERE class_date = ?",
                ROW_MAPPER, date);
    }

    public List<Attendance> findByRollNo(int rollNo) {
        return jdbc.query(
                "SELECT id, roll_no, class_date, status FROM attendance WHERE roll_no = ? ORDER BY class_date",
                ROW_MAPPER, rollNo);
    }

    public int countPresentByRollNo(int rollNo) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM attendance WHERE roll_no = ? AND status = 'P'",
                Integer.class, rollNo);
        return count == null ? 0 : count;
    }

    public int countTotalByRollNo(int rollNo) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM attendance WHERE roll_no = ?",
                Integer.class, rollNo);
        return count == null ? 0 : count;
    }

    @Transactional
    public void upsertBatch(LocalDate date, List<int[]> rollNoStatusPairs) {
        log.debug("Upserting {} attendance records for date={}", rollNoStatusPairs.size(), date);
        String sql = """
                INSERT INTO attendance (roll_no, class_date, status)
                VALUES (?, ?, ?)
                ON CONFLICT (roll_no, class_date) DO UPDATE SET status = EXCLUDED.status
                """;
        List<Object[]> batchArgs = rollNoStatusPairs.stream()
                .map(pair -> new Object[]{pair[0], date, String.valueOf((char) pair[1])})
                .toList();
        jdbc.batchUpdate(sql, batchArgs);
    }

    public List<DistinctDate> findDistinctDatesWithCounts() {
        String sql = """
                SELECT class_date,
                       COUNT(*) FILTER (WHERE status = 'P') AS present_count,
                       COUNT(*) FILTER (WHERE status = 'A') AS absent_count,
                       COUNT(*) AS total_count
                FROM attendance
                GROUP BY class_date
                ORDER BY class_date
                """;
        return jdbc.query(sql, (rs, rowNum) -> new DistinctDate(
                rs.getDate("class_date").toLocalDate(),
                rs.getInt("present_count"),
                rs.getInt("absent_count"),
                rs.getInt("total_count")
        ));
    }

    public record DistinctDate(LocalDate date, int present, int absent, int total) {}
}
