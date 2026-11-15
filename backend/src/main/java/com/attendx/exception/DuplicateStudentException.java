package com.attendx.exception;

public class DuplicateStudentException extends RuntimeException {
    public DuplicateStudentException(int rollNo) {
        super("Student already exists with roll number: " + rollNo);
    }
}
