package com.attendx.ui.controller;

import com.attendx.ui.model.AttendanceSummary;
import com.attendx.ui.service.AttendXService;
import com.attendx.ui.util.AnimationUtil;
import com.attendx.ui.util.ToastManager;
import com.attendx.ui.view.Scene3DLogo;
import com.attendx.ui.view.BarChart3D;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

public class DashboardController {

    private final AttendXService service;
    private final ToastManager toast;

    public DashboardController(AttendXService service, ToastManager toast) {
        this.service = service;
        this.toast = toast;
    }

    public Node buildView() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox root = new VBox(24);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #0B1020;");
        scroll.setContent(root);

        // Header
        Label title = new Label("Dashboard");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label subtitle = new Label("Real-time attendance overview");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #8899bb;");
        VBox header = new VBox(4, title, subtitle);

        // 3D hero logo scene
        Scene3DLogo logo3D = new Scene3DLogo();
        HBox heroBox = new HBox(logo3D.build());
        heroBox.setAlignment(Pos.CENTER);
        heroBox.setPrefHeight(220);

        // Stat cards row (placeholders)
        Label studentsCard    = statCard("Total Students", "0", "#6C5CE7");
        Label presentCard     = statCard("Present Today", "0", "#2ECC71");
        Label absentCard      = statCard("Absent Today", "0", "#FF4D6D");
        Label avgCard         = statCard("Class Average", "0%", "#00D2FF");

        HBox statsRow = new HBox(16, studentsCard, presentCard, absentCard, avgCard);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(studentsCard, Priority.ALWAYS);
        HBox.setHgrow(presentCard, Priority.ALWAYS);
        HBox.setHgrow(absentCard, Priority.ALWAYS);
        HBox.setHgrow(avgCard, Priority.ALWAYS);

        // 3D bar chart
        Label chartTitle = new Label("Attendance Trend (3D)");
        chartTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        BarChart3D barChart3D = new BarChart3D();
        Node chart3DNode = barChart3D.build();

        VBox chartSection = glassCard();
        chartSection.getChildren().addAll(chartTitle, chart3DNode);

        // Loading indicator
        ProgressIndicator loading = new ProgressIndicator();
        loading.setStyle("-fx-accent: #6C5CE7;");

        root.getChildren().addAll(header, heroBox, statsRow, loading, chartSection);

        // Load data async
        Task<AttendanceSummary> task = new Task<>() {
            @Override protected AttendanceSummary call() throws Exception {
                return service.getSummary();
            }
        };
        task.setOnSucceeded(e -> {
            AttendanceSummary summary = task.getValue();
            Platform.runLater(() -> {
                root.getChildren().remove(loading);
                int totalStudents = summary.students().size();
                double avg = summary.classAverage();

                // Count up animations
                AnimationUtil.countUp(findLabel(studentsCard), 0, totalStudents, "");
                AnimationUtil.countUp(findLabel(avgCard), 0, avg, "%");

                // Today's present/absent from trend
                if (!summary.trend().isEmpty()) {
                    var today = summary.trend().get(summary.trend().size() - 1);
                    AnimationUtil.countUp(findLabel(presentCard), 0, today.present(), "");
                    AnimationUtil.countUp(findLabel(absentCard), 0, today.absent(), "");
                }

                // Update chart
                barChart3D.updateData(summary.trend());

                // Stagger-animate stat cards
                for (int i = 0; i < statsRow.getChildren().size(); i++) {
                    AnimationUtil.fadeSlideIn(statsRow.getChildren().get(i), 30, i * 80);
                }
            });
        });
        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                root.getChildren().remove(loading);
                toast.showError("Failed to load dashboard data");
            });
        });
        new Thread(task).start();

        return scroll;
    }

    private Label statCard(String title, String value, String color) {
        Label val = new Label(value);
        val.setStyle(String.format("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: %s;", color));
        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #8899bb;");

        VBox card = new VBox(6, val, lbl);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(20));
        card.setStyle(String.format(
                "-fx-background-color: #121929; -fx-background-radius: 16; " +
                "-fx-border-color: rgba(108,92,231,0.2); -fx-border-radius: 16; " +
                "-fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 12, 0, 0, 4);"));
        // Store label as user-data for countUp access
        card.setUserData(val);

        Label wrapper = new Label();
        wrapper.setGraphic(card);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        wrapper.setUserData(val);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);

        return val; // return the value label directly for animation
    }

    private Label findLabel(Label statLabel) { return statLabel; }

    private VBox glassCard() {
        VBox card = new VBox(16);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: rgba(18,25,41,0.8); -fx-background-radius: 16; " +
                "-fx-border-color: rgba(108,92,231,0.15); -fx-border-radius: 16; -fx-border-width: 1;");
        return card;
    }
}
