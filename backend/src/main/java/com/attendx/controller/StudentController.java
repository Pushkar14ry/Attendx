package com.attendx.controller;

import com.attendx.dto.StudentRequest;
import com.attendx.dto.StudentResponse;
import com.attendx.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students")
@Tag(name = "Students", description = "Student management endpoints")
public class StudentController {

    private final StudentService service;

    public StudentController(StudentService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Add a new student")
    public ResponseEntity<StudentResponse> addStudent(@Valid @RequestBody StudentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addStudent(req));
    }

    @GetMapping
    @Operation(summary = "List all students")
    public List<StudentResponse> listStudents() {
        return service.listStudents();
    }

    @GetMapping("/{rollNo}")
    @Operation(summary = "Get student by roll number")
    public StudentResponse getStudent(@PathVariable("rollNo") int rollNo) {
        return service.getStudent(rollNo);
    }

    @DeleteMapping("/{rollNo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a student")
    public void deleteStudent(@PathVariable("rollNo") int rollNo) {
        service.deleteStudent(rollNo);
    }
}
