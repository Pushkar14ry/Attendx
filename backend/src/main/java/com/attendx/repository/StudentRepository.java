package com.attendx.repository;

import com.attendx.model.Student;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class StudentRepository {

    private static final Logger log = LoggerFactory.getLogger(StudentRepository.class);
    private final JdbcTemplate jdbc;

    public StudentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Student> ROW_MAPPER = (rs, rowNum) -> new Student(
            rs.getInt("roll_no"),
            rs.getString("name"),
            rs.getTimestamp("created_at").toLocalDateTime()
    );

    public List<Student> findAll() {
        log.debug("Fetching all students");
        return jdbc.query("SELECT roll_no, name, created_at FROM students ORDER BY roll_no", ROW_MAPPER);
    }

    public Optional<Student> findByRollNo(int rollNo) {
        log.debug("Fetching student with rollNo={}", rollNo);
        List<Student> results = jdbc.query(
                "SELECT roll_no, name, created_at FROM students WHERE roll_no = ?",
                ROW_MAPPER, rollNo);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public boolean existsByRollNo(int rollNo) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM students WHERE roll_no = ?", Integer.class, rollNo);
        return count != null && count > 0;
    }

    public Student save(int rollNo, String name) {
        log.debug("Inserting student rollNo={}, name={}", rollNo, name);
        jdbc.update("INSERT INTO students (roll_no, name) VALUES (?, ?)", rollNo, name);
        return findByRollNo(rollNo).orElseThrow();
    }

    public int deleteByRollNo(int rollNo) {
        log.debug("Deleting student rollNo={}", rollNo);
        return jdbc.update("DELETE FROM students WHERE roll_no = ?", rollNo);
    }
}
