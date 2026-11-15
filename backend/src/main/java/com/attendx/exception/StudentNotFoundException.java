package com.attendx.exception;

public class StudentNotFoundException extends RuntimeException {
    public StudentNotFoundException(int rollNo) {
        super("Student not found with roll number: " + rollNo);
    }
}
