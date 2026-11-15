-- V2__seed.sql: Seed data for AttendX
INSERT INTO students (roll_no, name) VALUES (1, 'Nihan')
  ON CONFLICT (roll_no) DO NOTHING;

INSERT INTO attendance (roll_no, class_date, status) VALUES (1, '2026-11-05', 'P')
  ON CONFLICT (roll_no, class_date) DO NOTHING;
