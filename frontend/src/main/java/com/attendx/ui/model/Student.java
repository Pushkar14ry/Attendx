package com.attendx.ui.model;

import java.time.LocalDateTime;

public record Student(int rollNo, String name, LocalDateTime createdAt) {}
