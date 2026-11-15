package com.attendx.ui.controller;

import com.attendx.ui.model.Student;
import com.attendx.ui.service.AttendXService;
import com.attendx.ui.util.AnimationUtil;
import com.attendx.ui.util.ToastManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.util.*;

public class MarkAttendanceController {

    private final AttendXService service;
    private final ToastManager toast;

    // Per-student toggle state: rollNo -> "P" or "A"
    private final Map<Integer, String> markMap = new LinkedHashMap<>();
    private List<Student> currentStudents = new ArrayList<>();
    private FlowPane studentCardsPane;
    private DatePicker datePicker;

    public MarkAttendanceController(AttendXService service, ToastManager toast) {
        this.service = service;
        this.toast = toast;
    }

    public Node buildView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #0B1020;");

        Label title = new Label("Mark Attendance");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Date picker + "Mark All Present"
        datePicker = new DatePicker(LocalDate.now());
        datePicker.setStyle("-fx-background-color: #1a2340; -fx-text-fill: white;");
        datePicker.valueProperty().addListener((obs, old, val) -> loadStudents());

        Button markAllBtn = new Button("✓ Mark All Present");
        markAllBtn.setStyle("-fx-background-color: rgba(46,204,113,0.2); -fx-text-fill: #2ECC71; " +
                "-fx-border-color: #2ECC71; -fx-border-radius: 10; -fx-background-radius: 10; " +
                "-fx-cursor: hand; -fx-padding: 8 16; -fx-font-weight: bold;");
        AnimationUtil.addHoverScale(markAllBtn, 1.05);
        markAllBtn.setOnAction(e -> markAllPresent());

        Button saveBtn = new Button("💾 Save Attendance");
        saveBtn.setStyle("-fx-background-color: #6C5CE7; -fx-text-fill: white; " +
                "-fx-background-radius: 10; -fx-padding: 10 20; -fx-font-weight: bold; -fx-cursor: hand;");
        AnimationUtil.addHoverScale(saveBtn, 1.05);
        saveBtn.setOnAction(e -> saveAttendance());

        HBox toolbar = new HBox(16, new Label("Date:"), datePicker, markAllBtn, new Region(), saveBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getChildren().get(0).setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
        HBox.setHgrow(toolbar.getChildren().get(4), Priority.ALWAYS);

        // Student cards grid
        studentCardsPane = new FlowPane();
        studentCardsPane.setHgap(16);
        studentCardsPane.setVgap(16);
        studentCardsPane.setPadding(new Insets(8));

        ScrollPane scroll = new ScrollPane(studentCardsPane);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().addAll(title, toolbar, scroll);
        loadStudents();
        return root;
    }

    private void loadStudents() {
        markMap.clear();
        studentCardsPane.getChildren().clear();

        // Loading shimmer
        for (int i = 0; i < 6; i++) {
            VBox shimmer = new VBox();
            shimmer.setPrefSize(160, 100);
            shimmer.setStyle("-fx-background-color: #1a2340; -fx-background-radius: 16;");
            studentCardsPane.getChildren().add(shimmer);
        }

        Task<List<Student>> task = new Task<>() {
            @Override protected List<Student> call() throws Exception {
                return service.listStudents();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            currentStudents = task.getValue();
            studentCardsPane.getChildren().clear();
            for (int i = 0; i < currentStudents.size(); i++) {
                Student s = currentStudents.get(i);
                markMap.put(s.rollNo(), "P"); // default Present
                VBox card = buildStudentCard(s);
                studentCardsPane.getChildren().add(card);
                AnimationUtil.fadeSlideIn(card, 20, i * 60);
            }
        }));
        task.setOnFailed(e -> Platform.runLater(() ->
                toast.showError("Failed to load students")));
        new Thread(task).start();
    }

    private VBox buildStudentCard(Student s) {
        Label rollLbl = new Label("Roll #" + s.rollNo());
        rollLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #8899bb;");

        Label nameLbl = new Label(s.name());
        nameLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Toggle button Present/Absent
        ToggleButton toggle = new ToggleButton("PRESENT");
        toggle.setSelected(true);
        styleToggle(toggle, true);

        toggle.setOnAction(e -> {
            boolean present = toggle.isSelected();
            toggle.setText(present ? "PRESENT" : "ABSENT");
            styleToggle(toggle, present);
            markMap.put(s.rollNo(), present ? "P" : "A");
        });

        VBox card = new VBox(10, rollLbl, nameLbl, toggle);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(160, 120);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #121929; -fx-background-radius: 16; " +
                "-fx-border-color: rgba(108,92,231,0.2); -fx-border-radius: 16; -fx-border-width: 1;");
        AnimationUtil.addHoverScale(card, 1.03);
        return card;
    }

    private void styleToggle(ToggleButton btn, boolean present) {
        if (present) {
            btn.setStyle("-fx-background-color: rgba(46,204,113,0.3); -fx-text-fill: #2ECC71; " +
                    "-fx-background-radius: 20; -fx-border-color: #2ECC71; -fx-border-radius: 20; " +
                    "-fx-padding: 5 16; -fx-font-weight: bold; -fx-cursor: hand;");
        } else {
            btn.setStyle("-fx-background-color: rgba(255,77,109,0.3); -fx-text-fill: #FF4D6D; " +
                    "-fx-background-radius: 20; -fx-border-color: #FF4D6D; -fx-border-radius: 20; " +
                    "-fx-padding: 5 16; -fx-font-weight: bold; -fx-cursor: hand;");
        }
    }

    private void markAllPresent() {
        markMap.replaceAll((k, v) -> "P");
        for (Node card : studentCardsPane.getChildren()) {
            if (card instanceof VBox vb) {
                vb.getChildren().stream()
                        .filter(n -> n instanceof ToggleButton)
                        .map(n -> (ToggleButton) n)
                        .forEach(tb -> {
                            tb.setSelected(true);
                            tb.setText("PRESENT");
                            styleToggle(tb, true);
                        });
            }
        }
        toast.showInfo("All set to Present");
    }

    private void saveAttendance() {
        if (markMap.isEmpty()) {
            toast.showError("No students to mark");
            return;
        }
        LocalDate date = datePicker.getValue();

        List<Map<String, Object>> marks = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : markMap.entrySet()) {
            marks.add(Map.of("rollNo", entry.getKey(), "status", entry.getValue()));
        }

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                service.markAttendance(date, marks);
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() ->
                toast.showSuccess("Attendance saved for " + date)));
        task.setOnFailed(e -> Platform.runLater(() ->
                toast.showError("Failed to save attendance: " + task.getException().getMessage())));
        new Thread(task).start();
    }
}
