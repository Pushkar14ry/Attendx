package com.attendx.service;

import com.attendx.dto.StudentRequest;
import com.attendx.dto.StudentResponse;
import com.attendx.exception.DuplicateStudentException;
import com.attendx.exception.StudentNotFoundException;
import com.attendx.model.Student;
import com.attendx.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentService {

    private static final Logger log = LoggerFactory.getLogger(StudentService.class);
    private final StudentRepository repo;

    public StudentService(StudentRepository repo) {
        this.repo = repo;
    }

    public StudentResponse addStudent(StudentRequest req) {
        log.info("Adding student rollNo={}, name={}", req.rollNo(), req.name());
        if (repo.existsByRollNo(req.rollNo())) {
            throw new DuplicateStudentException(req.rollNo());
        }
        Student student = repo.save(req.rollNo(), req.name().trim());
        return toResponse(student);
    }

    public List<StudentResponse> listStudents() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    public StudentResponse getStudent(int rollNo) {
        Student student = repo.findByRollNo(rollNo)
                .orElseThrow(() -> new StudentNotFoundException(rollNo));
        return toResponse(student);
    }

    public void deleteStudent(int rollNo) {
        log.info("Deleting student rollNo={}", rollNo);
        int deleted = repo.deleteByRollNo(rollNo);
        if (deleted == 0) {
            throw new StudentNotFoundException(rollNo);
        }
    }

    private StudentResponse toResponse(Student s) {
        return new StudentResponse(s.rollNo(), s.name(), s.createdAt());
    }
}
