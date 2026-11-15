package com.attendx.ui.model;

import java.time.LocalDateTime;

public record Student(int rollNo, String name, LocalDateTime createdAt) {
    public int getRollNo() { return rollNo; }
    public String getName() { return name; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
