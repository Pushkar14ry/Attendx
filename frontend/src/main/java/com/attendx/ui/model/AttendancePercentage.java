package com.attendx.ui.model;

public record AttendancePercentage(
    int rollNo,
    String name,
    int present,
    int total,
    double percentage,
    String message
) {
    public int getRollNo() { return rollNo; }
    public String getName() { return name; }
    public int getPresent() { return present; }
    public int getTotal() { return total; }
    public double getPercentage() { return percentage; }
    public String getMessage() { return message; }
}
