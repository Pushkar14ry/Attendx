-- V1__init.sql: Initial schema for AttendX
CREATE TABLE IF NOT EXISTS students (
  roll_no    INTEGER PRIMARY KEY,
  name       VARCHAR(100) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS attendance (
  id         BIGSERIAL PRIMARY KEY,
  roll_no    INTEGER NOT NULL REFERENCES students(roll_no) ON DELETE CASCADE,
  class_date DATE    NOT NULL,
  status     CHAR(1) NOT NULL CHECK (status IN ('P','A')),
  UNIQUE (roll_no, class_date)
);

CREATE INDEX IF NOT EXISTS idx_attendance_date ON attendance(class_date);
CREATE INDEX IF NOT EXISTS idx_attendance_roll_no ON attendance(roll_no);
