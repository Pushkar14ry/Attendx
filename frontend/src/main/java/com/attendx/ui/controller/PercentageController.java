package com.attendx.ui.controller;

import com.attendx.ui.model.AttendancePercentage;
import com.attendx.ui.model.Student;
import com.attendx.ui.service.AttendXService;
import com.attendx.ui.util.AnimationUtil;
import com.attendx.ui.util.ToastManager;
import com.attendx.ui.view.Gauge3D;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class PercentageController {

    private final AttendXService service;
    private final ToastManager toast;
    private ComboBox<Student> studentCombo;
    private Label nameLbl, presentLbl, totalLbl, missedLbl, pctLbl, statusBadge, msgLbl;
    private Gauge3D gauge3D;

    public PercentageController(AttendXService service, ToastManager toast) {
        this.service = service;
        this.toast = toast;
    }

    public Node buildView() {
        VBox root = new VBox(24);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #FAF8F5;");

        // Header
        Label title = new Label("Student Attendance Percentage");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #1B2B24;");
        Label subtitle = new Label("Select a student to inspect attendance rate, status, and 3D gauge");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #5E7267;");
        VBox header = new VBox(4, title, subtitle);

        // Student selector
        Label selectLbl = new Label("Student:");
        selectLbl.setStyle("-fx-text-fill: #1B2B24; -fx-font-size: 13px; -fx-font-weight: bold;");

        studentCombo = new ComboBox<>();
        studentCombo.setPromptText("Select a student…");
        studentCombo.setPrefWidth(280);
        studentCombo.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #CCD6CF; -fx-border-radius: 8; -fx-background-radius: 8;");
        studentCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Student s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? "" : "Roll #" + s.rollNo() + "  —  " + s.name());
                if (!empty && s != null) {
                    setStyle("-fx-text-fill: #1B2B24; -fx-font-weight: bold;");
                }
            }
        });
        studentCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Student s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? "Select a student…" : "Roll #" + s.rollNo() + "  —  " + s.name());
                if (!empty && s != null) {
                    setStyle("-fx-text-fill: #1B2B24; -fx-font-weight: bold;");
                }
            }
        });
        studentCombo.valueProperty().addListener((obs, old, val) -> {
            if (val != null) loadPercentageFor(val);
        });

        Button viewBtn = new Button("View Stats");
        viewBtn.setStyle("-fx-background-color: #4D735E; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 8 20; -fx-cursor: hand; -fx-font-weight: bold;");
        AnimationUtil.addHoverScale(viewBtn, 1.05);
        viewBtn.setOnAction(e -> {
            Student s = studentCombo.getValue();
            if (s != null) loadPercentageFor(s);
            else toast.showError("Please select a student from the dropdown");
        });

        HBox selector = new HBox(14, selectLbl, studentCombo, viewBtn);
        selector.setAlignment(Pos.CENTER_LEFT);

        // Result Card
        gauge3D = new Gauge3D();
        Node gaugeNode = gauge3D.build();

        nameLbl     = resultLabel("Select a student", 22, "#1B2B24");
        pctLbl      = resultLabel("—", 60, "#4D735E");
        statusBadge = new Label("NO DATA");
        statusBadge.setStyle("-fx-background-color: #EFECE5; -fx-text-fill: #5E7267; " +
                "-fx-padding: 5 16; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 12px; " +
                "-fx-border-color: #CCD6CF; -fx-border-radius: 12;");

        presentLbl  = resultLabel("✓ Classes Attended: —", 14, "#36825B");
        missedLbl   = resultLabel("✗ Classes Missed: —", 14, "#BA4E4E");
        totalLbl    = resultLabel("Total Classes: —", 14, "#5E7267");
        msgLbl      = resultLabel("", 12, "#B88235");

        VBox stats = new VBox(10, nameLbl, statusBadge, pctLbl, presentLbl, missedLbl, totalLbl, msgLbl);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.setPadding(new Insets(10, 24, 10, 24));

        HBox resultRow = new HBox(50, gaugeNode, stats);
        resultRow.setAlignment(Pos.CENTER);
        resultRow.setPadding(new Insets(20));

        VBox resultCard = new VBox(resultRow);
        resultCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 20; " +
                "-fx-border-color: #E3DED6; -fx-border-radius: 20; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(39,60,51,0.06), 16, 0, 0, 6);");
        resultCard.setAlignment(Pos.CENTER);

        // Guidelines card
        HBox guidelines = buildGuidelines();

        root.getChildren().addAll(header, selector, resultCard, guidelines);
        loadStudents();
        return root;
    }

    private HBox buildGuidelines() {
        Label g1 = statusIndicator("≥ 90% Excellent", "#2D6B49", "#E2F1E8", "#36825B");
        Label g2 = statusIndicator("75% - 89% Warning", "#B88235", "#FAF2E1", "#B88235");
        Label g3 = statusIndicator("< 75% Critical", "#BA4E4E", "#F9E8E8", "#BA4E4E");

        Label threshLbl = new Label("Thresholds:");
        threshLbl.setStyle("-fx-text-fill: #1B2B24; -fx-font-size: 12px; -fx-font-weight: bold;");

        HBox box = new HBox(16, threshLbl, g1, g2, g3);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(12, 20, 12, 20));
        box.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12; " +
                "-fx-border-color: #E3DED6; -fx-border-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(39,60,51,0.04), 8, 0, 0, 2);");
        return box;
    }

    private Label statusIndicator(String text, String textColor, String bgColor, String borderColor) {
        Label lbl = new Label(text);
        lbl.setStyle(String.format(
                "-fx-text-fill: %s; -fx-font-size: 12px; -fx-font-weight: bold; " +
                "-fx-padding: 4 12; -fx-background-color: %s; -fx-background-radius: 8; " +
                "-fx-border-color: %s; -fx-border-radius: 8;", textColor, bgColor, borderColor));
        return lbl;
    }

    private void loadStudents() {
        Task<List<Student>> task = new Task<>() {
            @Override
            protected List<Student> call() throws Exception {
                return service.listStudents();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            List<Student> students = task.getValue();
            studentCombo.getItems().setAll(students);
            if (!students.isEmpty() && studentCombo.getValue() == null) {
                studentCombo.setValue(students.get(0));
            }
        }));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex != null) ex.printStackTrace();
            Platform.runLater(() -> toast.showError("Failed to load students: " + (ex != null ? ex.getMessage() : "")));
        });
        new Thread(task).start();
    }

    private void loadPercentageFor(Student selected) {
        Task<AttendancePercentage> task = new Task<>() {
            @Override
            protected AttendancePercentage call() throws Exception {
                return service.getPercentage(selected.rollNo());
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            AttendancePercentage pct = task.getValue();
            nameLbl.setText(pct.name() + "  (Roll #" + pct.rollNo() + ")");
            presentLbl.setText("✓ Classes Attended: " + pct.present());
            missedLbl.setText("✗ Classes Missed: " + (pct.total() - pct.present()));
            totalLbl.setText("Total Classes: " + pct.total());
            msgLbl.setText(pct.message() != null ? pct.message() : "");

            String color;
            String textColor;
            String bgColor;
            String borderColor;
            String badgeText;
            if (pct.total() == 0) {
                color = "#5E7267";
                textColor = "#5E7267";
                bgColor = "#EFECE5";
                borderColor = "#CCD6CF";
                badgeText = "NO RECORDS";
            } else if (pct.percentage() < 75.0) {
                color = "#BA4E4E";
                textColor = "#BA4E4E";
                bgColor = "#F9E8E8";
                borderColor = "#BA4E4E";
                badgeText = "⚠ CRITICAL (< 75%)";
            } else if (pct.percentage() < 90.0) {
                color = "#B88235";
                textColor = "#B88235";
                bgColor = "#FAF2E1";
                borderColor = "#B88235";
                badgeText = "⚡ WARNING (75-89%)";
            } else {
                color = "#36825B";
                textColor = "#2D6B49";
                bgColor = "#E2F1E8";
                borderColor = "#36825B";
                badgeText = "★ EXCELLENT (≥ 90%)";
            }

            statusBadge.setText(badgeText);
            statusBadge.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; " +
                    "-fx-padding: 5 16; -fx-background-radius: 12; -fx-font-weight: bold; " +
                    "-fx-border-color: %s; -fx-border-radius: 12;",
                    bgColor, textColor, borderColor));

            pctLbl.setStyle("-fx-font-size: 60px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
            AnimationUtil.countUp(pctLbl, 0, pct.percentage(), "%");
            gauge3D.animateTo(pct.percentage(), color);
        }));

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex != null) ex.printStackTrace();
            Platform.runLater(() -> toast.showError("Failed to calculate percentage: " + (ex != null ? ex.getMessage() : "")));
        });

        new Thread(task).start();
    }

    private Label resultLabel(String text, int size, String color) {
        Label lbl = new Label(text);
        lbl.setStyle(String.format("-fx-font-size: %dpx; -fx-text-fill: %s; -fx-font-weight: bold;", size, color));
        return lbl;
    }
}
