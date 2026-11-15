package com.attendx.ui.controller;

import com.attendx.ui.model.DailyReport;
import com.attendx.ui.service.AttendXService;
import com.attendx.ui.util.AnimationUtil;
import com.attendx.ui.util.ToastManager;
import com.attendx.ui.view.BarChart3D;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;

public class DailyReportController {

    private final AttendXService service;
    private final ToastManager toast;
    private TableView<DailyReport.StudentStatus> table;
    private DatePicker datePicker;
    private Label presentLbl, absentLbl, notMarkedLbl, rateLbl;
    private BarChart3D barChart3D;

    public DailyReportController(AttendXService service, ToastManager toast) {
        this.service = service;
        this.toast = toast;
    }

    public Node buildView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #FAF8F5;");

        // Header
        Label title = new Label("Daily Attendance Report");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #1B2B24;");
        Label subtitle = new Label("Inspect daily roster statuses and 3D visual distribution");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #5E7267;");
        VBox header = new VBox(4, title, subtitle);

        // Date picker toolbar
        Label dateLbl = new Label("Select Date:");
        dateLbl.setStyle("-fx-text-fill: #1B2B24; -fx-font-size: 13px; -fx-font-weight: bold;");

        datePicker = new DatePicker(LocalDate.now());
        datePicker.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #1B2B24; -fx-border-color: #CCD6CF; -fx-border-radius: 8;");
        datePicker.valueProperty().addListener((obs, old, val) -> loadReport());

        Button refreshBtn = new Button("↻ Refresh");
        refreshBtn.setStyle("-fx-background-color: #4D735E; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 8 20; -fx-cursor: hand; -fx-font-weight: bold;");
        AnimationUtil.addHoverScale(refreshBtn, 1.05);
        refreshBtn.setOnAction(e -> loadReport());

        HBox toolbar = new HBox(14, dateLbl, datePicker, refreshBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // Summary chips row
        presentLbl   = summaryBadge("✓ Present: 0", "#2D6B49", "#E2F1E8", "#36825B");
        absentLbl    = summaryBadge("✗ Absent: 0", "#BA4E4E", "#F9E8E8", "#BA4E4E");
        notMarkedLbl = summaryBadge("— Not Marked: 0", "#B88235", "#FAF2E1", "#B88235");
        rateLbl      = summaryBadge("Attendance: 0%", "#273C33", "#EBF2EC", "#4D735E");
        HBox summary = new HBox(12, presentLbl, absentLbl, notMarkedLbl, rateLbl);

        // 3D Chart
        barChart3D = new BarChart3D();
        Node chartNode = barChart3D.build();

        VBox chartBox = new VBox(10);
        Label chartHeading = new Label("Daily Distribution (Interactive 3D)");
        chartHeading.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1B2B24;");
        Label chartHint = new Label("Green: Present • Red: Absent • Amber: Not Marked");
        chartHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #5E7267;");
        chartBox.getChildren().addAll(chartHeading, chartHint, chartNode);
        chartBox.setPadding(new Insets(16));
        chartBox.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16; " +
                "-fx-border-color: #E3DED6; -fx-border-radius: 16; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(39,60,51,0.06), 14, 0, 0, 4);");

        // Table
        table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        HBox midSplit = new HBox(16, table, chartBox);
        HBox.setHgrow(table, Priority.ALWAYS);
        HBox.setHgrow(chartBox, Priority.NEVER);
        VBox.setVgrow(midSplit, Priority.ALWAYS);

        root.getChildren().addAll(header, toolbar, summary, midSplit);
        loadReport();
        return root;
    }

    @SuppressWarnings("unchecked")
    private TableView<DailyReport.StudentStatus> buildTable() {
        TableColumn<DailyReport.StudentStatus, Integer> rollCol = new TableColumn<>("Roll No");
        rollCol.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().rollNo()));
        rollCol.setPrefWidth(90);
        rollCol.setStyle("-fx-alignment: CENTER;");
        rollCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer roll, boolean empty) {
                super.updateItem(roll, empty);
                if (empty || roll == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label("#" + roll);
                    badge.setStyle("-fx-background-color: #E8F0EC; -fx-text-fill: #2D5540; " +
                            "-fx-font-weight: bold; -fx-padding: 3 10; -fx-background-radius: 8; -fx-font-size: 12px;");
                    setGraphic(badge);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                }
            }
        });

        TableColumn<DailyReport.StudentStatus, String> nameCol = new TableColumn<>("Student Name");
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().name()));
        nameCol.setPrefWidth(220);
        nameCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #1B2B24; -fx-font-weight: bold; -fx-font-size: 13px;");
                }
            }
        });

        TableColumn<DailyReport.StudentStatus, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label chip = new Label(item);
                if ("PRESENT".equalsIgnoreCase(item)) {
                    chip.setStyle("-fx-background-color: #E2F1E8; -fx-text-fill: #2D6B49; " +
                            "-fx-background-radius: 20; -fx-border-color: #36825B; -fx-border-radius: 20; " +
                            "-fx-padding: 4 14; -fx-font-weight: bold; -fx-font-size: 11px;");
                } else if ("ABSENT".equalsIgnoreCase(item)) {
                    chip.setStyle("-fx-background-color: #F9E8E8; -fx-text-fill: #BA4E4E; " +
                            "-fx-background-radius: 20; -fx-border-color: #BA4E4E; -fx-border-radius: 20; " +
                            "-fx-padding: 4 14; -fx-font-weight: bold; -fx-font-size: 11px;");
                } else {
                    chip.setStyle("-fx-background-color: #FAF2E1; -fx-text-fill: #B88235; " +
                            "-fx-background-radius: 20; -fx-border-color: #B88235; -fx-border-radius: 20; " +
                            "-fx-padding: 4 14; -fx-font-weight: bold; -fx-font-size: 11px;");
                }
                setGraphic(chip);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        });
        statusCol.setPrefWidth(130);

        TableView<DailyReport.StudentStatus> tv = new TableView<>();
        tv.getColumns().addAll(rollCol, nameCol, statusCol);
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tv.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 14; " +
                "-fx-border-color: #E3DED6; -fx-border-radius: 14; " +
                "-fx-effect: dropshadow(gaussian, rgba(39,60,51,0.06), 14, 0, 0, 3);");
        Label placeholder = new Label("No attendance records for this date");
        placeholder.setStyle("-fx-text-fill: #5E7267; -fx-font-size: 13px;");
        tv.setPlaceholder(placeholder);
        return tv;
    }

    private void loadReport() {
        LocalDate date = datePicker.getValue();
        if (date == null) return;

        Task<DailyReport> task = new Task<>() {
            @Override
            protected DailyReport call() throws Exception {
                return service.getDailyReport(date);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            DailyReport report = task.getValue();
            table.setItems(FXCollections.observableArrayList(report.students()));
            presentLbl.setText("✓ Present: " + report.present());
            absentLbl.setText("✗ Absent: " + report.absent());
            notMarkedLbl.setText("— Not Marked: " + report.notMarked());

            int totalMarked = report.present() + report.absent();
            double pct = totalMarked > 0 ? (report.present() * 100.0 / totalMarked) : 0.0;
            rateLbl.setText(String.format("Attendance: %.1f%%", pct));

            // Update 3D bar chart
            barChart3D.updateDailyData(report.present(), report.absent(), report.notMarked());
        }));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex != null) ex.printStackTrace();
            Platform.runLater(() ->
                toast.showError("Failed to load daily report: " + (ex != null ? ex.getMessage() : "")));
        });
        new Thread(task).start();
    }

    private Label summaryBadge(String text, String textColor, String bgColor, String borderColor) {
        Label lbl = new Label(text);
        lbl.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; " +
                "-fx-background-radius: 20; -fx-border-color: %s; -fx-border-radius: 20; " +
                "-fx-padding: 7 18; -fx-font-weight: bold; -fx-font-size: 13px;", bgColor, textColor, borderColor));
        return lbl;
    }
}
