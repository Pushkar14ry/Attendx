package com.attendx.ui.controller;

import com.attendx.ui.model.DailyReport;
import com.attendx.ui.model.Student;
import com.attendx.ui.service.AttendXService;
import com.attendx.ui.util.AnimationUtil;
import com.attendx.ui.util.ToastManager;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.time.LocalDate;
import java.util.*;

public class MarkAttendanceController {

    private final AttendXService service;
    private final ToastManager toast;

    // Per-student toggle state: rollNo -> "P" or "A"
    private final Map<Integer, String> markMap = new LinkedHashMap<>();
    private final Map<Integer, ToggleButton> toggleMap = new HashMap<>();
    private List<Student> currentStudents = new ArrayList<>();
    private FlowPane studentCardsPane;
    private DatePicker datePicker;
    private TextField searchField;
    private Button saveBtn;
    private Label countStatsLabel;

    public MarkAttendanceController(AttendXService service, ToastManager toast) {
        this.service = service;
        this.toast = toast;
    }

    public Node buildView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #FAF8F5;");

        // Header
        Label title = new Label("Mark Attendance");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #1B2B24;");
        Label subtitle = new Label("Select a date, toggle student statuses, and batch-save to database");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #5E7267;");
        VBox header = new VBox(4, title, subtitle);

        // Date picker + Toolbar
        Label dateLbl = new Label("Class Date:");
        dateLbl.setStyle("-fx-text-fill: #1B2B24; -fx-font-size: 13px; -fx-font-weight: bold;");

        datePicker = new DatePicker(LocalDate.now());
        datePicker.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #1B2B24; -fx-border-color: #CCD6CF; -fx-border-radius: 8;");
        datePicker.valueProperty().addListener((obs, old, val) -> loadStudentsAndMarks());

        Button markAllPBtn = new Button("✓ All Present");
        markAllPBtn.setStyle("-fx-background-color: #E2F1E8; -fx-text-fill: #2D6B49; " +
                "-fx-border-color: #36825B; -fx-border-radius: 8; -fx-background-radius: 8; " +
                "-fx-cursor: hand; -fx-padding: 8 16; -fx-font-weight: bold;");
        AnimationUtil.addHoverScale(markAllPBtn, 1.04);
        markAllPBtn.setOnAction(e -> setAllStatus("P"));

        Button markAllABtn = new Button("✗ All Absent");
        markAllABtn.setStyle("-fx-background-color: #F9E8E8; -fx-text-fill: #BA4E4E; " +
                "-fx-border-color: #BA4E4E; -fx-border-radius: 8; -fx-background-radius: 8; " +
                "-fx-cursor: hand; -fx-padding: 8 16; -fx-font-weight: bold;");
        AnimationUtil.addHoverScale(markAllABtn, 1.04);
        markAllABtn.setOnAction(e -> setAllStatus("A"));

        searchField = new TextField();
        searchField.setPromptText("Filter student…");
        searchField.setPrefWidth(180);
        searchField.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #1B2B24; " +
                "-fx-prompt-text-fill: #8E9F95; " +
                "-fx-border-color: #CCD6CF; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 12;");
        searchField.textProperty().addListener((obs, old, val) -> filterCards(val));

        saveBtn = new Button("💾 Save Attendance");
        saveBtn.setStyle("-fx-background-color: #4D735E; -fx-text-fill: white; " +
                "-fx-background-radius: 10; -fx-padding: 10 22; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 13px;");
        AnimationUtil.addHoverScale(saveBtn, 1.05);
        saveBtn.setOnAction(e -> saveAttendance());

        countStatsLabel = new Label("Present: 0 | Absent: 0");
        countStatsLabel.setStyle("-fx-text-fill: #5E7267; -fx-font-size: 13px; -fx-font-weight: bold; " +
                "-fx-background-color: #EFECE5; -fx-padding: 6 12; -fx-background-radius: 8;");

        HBox toolbar = new HBox(12,
                dateLbl, datePicker,
                markAllPBtn, markAllABtn,
                searchField,
                new Region(),
                countStatsLabel,
                saveBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(toolbar.getChildren().get(5), Priority.ALWAYS);

        // Student cards grid
        studentCardsPane = new FlowPane();
        studentCardsPane.setHgap(16);
        studentCardsPane.setVgap(16);
        studentCardsPane.setPadding(new Insets(12));

        ScrollPane scroll = new ScrollPane(studentCardsPane);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().addAll(header, toolbar, scroll);
        loadStudentsAndMarks();
        return root;
    }

    private void loadStudentsAndMarks() {
        markMap.clear();
        toggleMap.clear();
        studentCardsPane.getChildren().clear();

        LocalDate selectedDate = datePicker.getValue();
        if (selectedDate == null) return;

        Label loadingLbl = new Label("Loading roster and existing marks for " + selectedDate + "...");
        loadingLbl.setStyle("-fx-text-fill: #5E7267; -fx-font-size: 14px;");
        studentCardsPane.getChildren().add(loadingLbl);

        Task<Map<String, Object>> task = new Task<>() {
            @Override
            protected Map<String, Object> call() throws Exception {
                List<Student> students = service.listStudents();
                DailyReport report = null;
                try {
                    report = service.getDailyReport(selectedDate);
                } catch (Exception ignored) {}
                Map<String, Object> res = new HashMap<>();
                res.put("students", students);
                res.put("report", report);
                return res;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            studentCardsPane.getChildren().clear();
            Map<String, Object> data = task.getValue();
            currentStudents = (List<Student>) data.get("students");
            DailyReport report = (DailyReport) data.get("report");

            Map<Integer, String> existingMarks = new HashMap<>();
            if (report != null && report.students() != null) {
                for (DailyReport.StudentStatus st : report.students()) {
                    if ("PRESENT".equalsIgnoreCase(st.status())) {
                        existingMarks.put(st.rollNo(), "P");
                    } else if ("ABSENT".equalsIgnoreCase(st.status())) {
                        existingMarks.put(st.rollNo(), "A");
                    }
                }
            }

            for (int i = 0; i < currentStudents.size(); i++) {
                Student s = currentStudents.get(i);
                // Pre-fill existing status if already marked, otherwise default to Present
                String initialStatus = existingMarks.getOrDefault(s.rollNo(), "P");
                markMap.put(s.rollNo(), initialStatus);

                VBox card = buildStudentCard(s, initialStatus);
                studentCardsPane.getChildren().add(card);
                AnimationUtil.fadeSlideIn(card, 16, i * 40);
            }
            updateStats();
        }));

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex != null) ex.printStackTrace();
            Platform.runLater(() -> toast.showError("Failed to load students: " + (ex != null ? ex.getMessage() : "")));
        });

        new Thread(task).start();
    }

    private VBox buildStudentCard(Student s, String initialStatus) {
        String initials = s.name().length() > 0 ? s.name().substring(0, 1).toUpperCase() : "?";
        Label avatar = new Label(initials);
        avatar.setStyle("-fx-background-color: #E2ECE5; -fx-text-fill: #3D5E4C; " +
                "-fx-font-weight: bold; -fx-font-size: 15px; -fx-background-radius: 20; " +
                "-fx-alignment: center; -fx-min-width: 38; -fx-min-height: 38; -fx-border-color: #CCD6CF; -fx-border-radius: 20;");

        Label rollLbl = new Label("Roll #" + s.rollNo());
        rollLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #5E7267; -fx-font-weight: bold;");

        Label nameLbl = new Label(s.name());
        nameLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1B2B24;");
        nameLbl.setMaxWidth(140);
        nameLbl.setWrapText(true);

        VBox infoBox = new VBox(2, rollLbl, nameLbl);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        HBox topRow = new HBox(10, avatar, infoBox);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Toggle button Present/Absent
        boolean isP = "P".equalsIgnoreCase(initialStatus);
        ToggleButton toggle = new ToggleButton(isP ? "PRESENT" : "ABSENT");
        toggle.setSelected(isP);
        styleToggle(toggle, isP);
        toggleMap.put(s.rollNo(), toggle);

        toggle.setOnAction(e -> {
            boolean present = toggle.isSelected();
            toggle.setText(present ? "PRESENT" : "ABSENT");
            styleToggle(toggle, present);
            markMap.put(s.rollNo(), present ? "P" : "A");
            animateToggle(toggle);
            updateStats();
        });

        VBox card = new VBox(12, topRow, toggle);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(190, 135);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 14; " +
                "-fx-border-color: #E3DED6; -fx-border-radius: 14; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(39,60,51,0.06), 10, 0, 0, 3);");
        card.setUserData(s);
        AnimationUtil.addHoverScale(card, 1.03);
        return card;
    }

    private void animateToggle(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(80), node);
        tt.setFromX(-3);
        tt.setToX(0);
        tt.play();
    }

    private void styleToggle(ToggleButton btn, boolean present) {
        if (present) {
            btn.setStyle("-fx-background-color: #E2F1E8; -fx-text-fill: #2D6B49; " +
                    "-fx-background-radius: 20; -fx-border-color: #36825B; -fx-border-radius: 20; " +
                    "-fx-padding: 6 18; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 12px;");
        } else {
            btn.setStyle("-fx-background-color: #F9E8E8; -fx-text-fill: #BA4E4E; " +
                    "-fx-background-radius: 20; -fx-border-color: #BA4E4E; -fx-border-radius: 20; " +
                    "-fx-padding: 6 18; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 12px;");
        }
    }

    private void setAllStatus(String status) {
        boolean isP = "P".equalsIgnoreCase(status);
        markMap.replaceAll((k, v) -> status);
        for (ToggleButton tb : toggleMap.values()) {
            tb.setSelected(isP);
            tb.setText(isP ? "PRESENT" : "ABSENT");
            styleToggle(tb, isP);
        }
        updateStats();
        toast.showInfo("All marked as " + (isP ? "Present" : "Absent"));
    }

    private void filterCards(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        for (Node n : studentCardsPane.getChildren()) {
            if (n instanceof VBox card && card.getUserData() instanceof Student s) {
                boolean match = q.isEmpty() ||
                        s.name().toLowerCase().contains(q) ||
                        String.valueOf(s.rollNo()).contains(q);
                card.setVisible(match);
                card.setManaged(match);
            }
        }
    }

    private void updateStats() {
        long pCount = markMap.values().stream().filter("P"::equalsIgnoreCase).count();
        long aCount = markMap.size() - pCount;
        countStatsLabel.setText(String.format("Present: %d  |  Absent: %d", pCount, aCount));
    }

    private void saveAttendance() {
        if (markMap.isEmpty()) {
            toast.showError("No students to mark");
            return;
        }
        LocalDate date = datePicker.getValue();
        if (date == null) {
            toast.showError("Please pick a valid date");
            return;
        }

        List<Map<String, Object>> marks = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : markMap.entrySet()) {
            marks.add(Map.of("rollNo", entry.getKey(), "status", entry.getValue()));
        }

        saveBtn.setDisable(true);
        saveBtn.setText("Saving...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                service.markAttendance(date, marks);
                return null;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            saveBtn.setDisable(false);
            saveBtn.setText("✓ Saved!");
            saveBtn.setStyle("-fx-background-color: #36825B; -fx-text-fill: white; " +
                    "-fx-background-radius: 10; -fx-padding: 10 22; -fx-font-weight: bold; -fx-font-size: 13px;");
            AnimationUtil.successBounce(saveBtn);
            toast.showSuccess("Attendance recorded for " + date);

            new Thread(() -> {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> {
                    saveBtn.setText("💾 Save Attendance");
                    saveBtn.setStyle("-fx-background-color: #4D735E; -fx-text-fill: white; " +
                            "-fx-background-radius: 10; -fx-padding: 10 22; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 13px;");
                });
            }).start();
        }));

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex != null) ex.printStackTrace();
            Platform.runLater(() -> {
                saveBtn.setDisable(false);
                saveBtn.setText("💾 Save Attendance");
                toast.showError("Save failed: " + (ex != null ? ex.getMessage() : "network error"));
            });
        });

        new Thread(task).start();
    }
}
