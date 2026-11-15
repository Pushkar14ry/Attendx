package com.attendx.model;

import java.time.LocalDateTime;

public record Student(
    Integer rollNo,
    String name,
    LocalDateTime createdAt
) {}
