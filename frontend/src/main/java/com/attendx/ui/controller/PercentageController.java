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
    private Label nameLbl, presentLbl, totalLbl, pctLbl, msgLbl;
    private Gauge3D gauge3D;

    public PercentageController(AttendXService service, ToastManager toast) {
        this.service = service;
        this.toast = toast;
    }

    public Node buildView() {
        VBox root = new VBox(24);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #0B1020;");

        Label title = new Label("Student Percentage");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Student selector
        studentCombo = new ComboBox<>();
        studentCombo.setPromptText("Select a student…");
        studentCombo.setPrefWidth(260);
        studentCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Student s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? "" : "#" + s.rollNo() + " — " + s.name());
            }
        });
        studentCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Student s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? "Select a student…" : "#" + s.rollNo() + " — " + s.name());
            }
        });

        Button viewBtn = new Button("View Percentage");
        viewBtn.setStyle("-fx-background-color: #6C5CE7; -fx-text-fill: white; " +
                "-fx-background-radius: 10; -fx-padding: 8 20; -fx-cursor: hand;");
        AnimationUtil.addHoverScale(viewBtn, 1.05);
        viewBtn.setOnAction(e -> loadPercentage());

        HBox selector = new HBox(12, new Label("Student:"), studentCombo, viewBtn);
        selector.setAlignment(Pos.CENTER_LEFT);
        selector.getChildren().get(0).setStyle("-fx-text-fill: white; -fx-font-size: 13px;");

        // Result card
        gauge3D = new Gauge3D();
        Node gaugeNode = gauge3D.build();

        nameLbl    = resultLabel("", 20, "white");
        pctLbl     = resultLabel("—", 56, "#00D2FF");
        pctLbl.setStyle("-fx-font-size: 56px; -fx-font-weight: bold; -fx-text-fill: #00D2FF;");
        presentLbl = resultLabel("", 14, "#8899bb");
        totalLbl   = resultLabel("", 14, "#8899bb");
        msgLbl     = resultLabel("", 12, "#FFC857");

        VBox stats = new VBox(8, nameLbl, pctLbl, presentLbl, totalLbl, msgLbl);
        stats.setAlignment(Pos.CENTER);

        HBox resultRow = new HBox(40, gaugeNode, stats);
        resultRow.setAlignment(Pos.CENTER);
        resultRow.setPadding(new Insets(20));

        VBox resultCard = new VBox(resultRow);
        resultCard.setStyle("-fx-background-color: #121929; -fx-background-radius: 20; " +
                "-fx-border-color: rgba(108,92,231,0.2); -fx-border-radius: 20; -fx-border-width: 1;");
        resultCard.setAlignment(Pos.CENTER);
        resultCard.setPadding(new Insets(24));

        root.getChildren().addAll(title, selector, resultCard);
        loadStudents();
        return root;
    }

    private void loadStudents() {
        Task<List<Student>> task = new Task<>() {
            @Override protected List<Student> call() throws Exception {
                return service.listStudents();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() ->
                studentCombo.getItems().setAll(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() ->
                toast.showError("Failed to load students")));
        new Thread(task).start();
    }

    private void loadPercentage() {
        Student selected = studentCombo.getValue();
        if (selected == null) { toast.showError("Please select a student"); return; }

        Task<AttendancePercentage> task = new Task<>() {
            @Override protected AttendancePercentage call() throws Exception {
                return service.getPercentage(selected.rollNo());
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            AttendancePercentage pct = task.getValue();
            nameLbl.setText(pct.name());
            presentLbl.setText("Present: " + pct.present());
            totalLbl.setText("Total classes: " + pct.total());
            msgLbl.setText(pct.message() != null ? pct.message() : "");

            // Colour-code by threshold
            String color;
            if (pct.percentage() < 75) color = "#FF4D6D";
            else if (pct.percentage() < 90) color = "#FFC857";
            else color = "#2ECC71";
            pctLbl.setStyle("-fx-font-size: 56px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

            // Animate count-up for percentage
            AnimationUtil.countUp(pctLbl, 0, pct.percentage(), "%");

            // Animate 3D gauge
            gauge3D.animateTo(pct.percentage(), color);
        }));
        task.setOnFailed(e -> Platform.runLater(() ->
                toast.showError("Failed to load percentage")));
        new Thread(task).start();
    }

    private Label resultLabel(String text, int size, String color) {
        Label lbl = new Label(text);
        lbl.setStyle(String.format("-fx-font-size: %dpx; -fx-text-fill: %s;", size, color));
        return lbl;
    }
}
