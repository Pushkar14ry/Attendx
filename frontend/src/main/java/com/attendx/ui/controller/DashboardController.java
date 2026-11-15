package com.attendx.ui.controller;

import com.attendx.ui.model.AttendanceSummary;
import com.attendx.ui.service.AttendXService;
import com.attendx.ui.util.AnimationUtil;
import com.attendx.ui.util.ToastManager;
import com.attendx.ui.view.BarChart3D;
import com.attendx.ui.view.Scene3DLogo;
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
        root.setStyle("-fx-background-color: #FAF8F5;");
        scroll.setContent(root);

        // Header
        Label title = new Label("Dashboard");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #1B2B24;");
        Label subtitle = new Label("Real-time attendance overview & 3D telemetry");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #5E7267;");
        VBox header = new VBox(4, title, subtitle);

        // 3D hero logo scene
        Scene3DLogo logo3D = new Scene3DLogo();
        HBox heroBox = new HBox(logo3D.build());
        heroBox.setAlignment(Pos.CENTER);
        heroBox.setPrefHeight(220);
        heroBox.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16; " +
                "-fx-border-color: #E3DED6; -fx-border-radius: 16; " +
                "-fx-effect: dropshadow(gaussian, rgba(39,60,51,0.06), 14, 0, 0, 3);");

        // Stat cards row
        Label studentsVal = new Label("0");
        Label presentVal  = new Label("0");
        Label absentVal   = new Label("0");
        Label avgVal      = new Label("0%");

        VBox studentsCard = buildStatCard("Total Students", studentsVal, "#273C33");
        VBox presentCard  = buildStatCard("Present Today", presentVal, "#36825B");
        VBox absentCard   = buildStatCard("Absent Today", absentVal, "#BA4E4E");
        VBox avgCard      = buildStatCard("Class Average", avgVal, "#4D735E");

        HBox statsRow = new HBox(16, studentsCard, presentCard, absentCard, avgCard);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(studentsCard, Priority.ALWAYS);
        HBox.setHgrow(presentCard, Priority.ALWAYS);
        HBox.setHgrow(absentCard, Priority.ALWAYS);
        HBox.setHgrow(avgCard, Priority.ALWAYS);

        // 3D bar chart
        Label chartTitle = new Label("Attendance Trend (Interactive 3D)");
        chartTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1B2B24;");
        Label chartSub = new Label("Drag to rotate • Scroll to zoom");
        chartSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #5E7267;");
        HBox chartHead = new HBox(10, chartTitle, chartSub);
        chartHead.setAlignment(Pos.BASELINE_LEFT);

        BarChart3D barChart3D = new BarChart3D();
        Node chart3DNode = barChart3D.build();

        VBox chartSection = card();
        chartSection.getChildren().addAll(chartHead, chart3DNode);

        // Loading indicator
        ProgressIndicator loading = new ProgressIndicator();
        loading.setStyle("-fx-accent: #4D735E;");

        root.getChildren().addAll(header, heroBox, statsRow, loading, chartSection);

        // Load data async
        Task<AttendanceSummary> task = new Task<>() {
            @Override
            protected AttendanceSummary call() throws Exception {
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
                AnimationUtil.countUp(studentsVal, 0, totalStudents, "");
                AnimationUtil.countUp(avgVal, 0, avg, "%");

                // Today's present/absent from trend
                if (!summary.trend().isEmpty()) {
                    var today = summary.trend().get(summary.trend().size() - 1);
                    AnimationUtil.countUp(presentVal, 0, today.present(), "");
                    AnimationUtil.countUp(absentVal, 0, today.absent(), "");
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
            Throwable ex = task.getException();
            if (ex != null) ex.printStackTrace();
            Platform.runLater(() -> {
                root.getChildren().remove(loading);
                toast.showError("Failed to load dashboard data: " + (ex != null ? ex.getMessage() : "unknown"));
            });
        });
        new Thread(task).start();

        return scroll;
    }

    private VBox buildStatCard(String title, Label valLabel, String color) {
        valLabel.setStyle(String.format("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: %s;", color));
        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #5E7267; -fx-font-weight: bold;");

        VBox card = new VBox(6, valLabel, lbl);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16; " +
                "-fx-border-color: #E3DED6; -fx-border-radius: 16; " +
                "-fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(39,60,51,0.06), 14, 0, 0, 4);");
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private VBox card() {
        VBox card = new VBox(16);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16; " +
                "-fx-border-color: #E3DED6; -fx-border-radius: 16; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(39,60,51,0.06), 14, 0, 0, 4);");
        return card;
    }
}
