package com.attendx.dto;

import java.time.LocalDateTime;

public record StudentResponse(
    Integer rollNo,
    String name,
    LocalDateTime createdAt
) {}
