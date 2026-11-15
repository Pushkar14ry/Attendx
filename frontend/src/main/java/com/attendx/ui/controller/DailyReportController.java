package com.attendx.ui.controller;

import com.attendx.ui.model.DailyReport;
import com.attendx.ui.service.AttendXService;
import com.attendx.ui.util.AnimationUtil;
import com.attendx.ui.util.ToastManager;
import com.attendx.ui.view.BarChart3D;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.time.LocalDate;
import java.util.List;

public class DailyReportController {

    private final AttendXService service;
    private final ToastManager toast;
    private TableView<DailyReport.StudentStatus> table;
    private DatePicker datePicker;
    private Label presentLbl, absentLbl, notMarkedLbl;

    public DailyReportController(AttendXService service, ToastManager toast) {
        this.service = service;
        this.toast = toast;
    }

    public Node buildView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #0B1020;");

        Label title = new Label("Daily Report");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: white;");

        datePicker = new DatePicker(LocalDate.now());
        datePicker.setStyle("-fx-background-color: #1a2340; -fx-text-fill: white;");

        Button loadBtn = new Button("Load Report");
        loadBtn.setStyle("-fx-background-color: #6C5CE7; -fx-text-fill: white; " +
                "-fx-background-radius: 10; -fx-padding: 8 20; -fx-cursor: hand;");
        AnimationUtil.addHoverScale(loadBtn, 1.05);
        loadBtn.setOnAction(e -> loadReport());

        HBox toolbar = new HBox(16, new Label("Date:"), datePicker, loadBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getChildren().get(0).setStyle("-fx-text-fill: white; -fx-font-size: 13px;");

        // Summary chips
        presentLbl   = chip("Present: 0", "#2ECC71");
        absentLbl    = chip("Absent: 0", "#FF4D6D");
        notMarkedLbl = chip("Not Marked: 0", "#FFC857");
        HBox summary = new HBox(12, presentLbl, absentLbl, notMarkedLbl);

        // Table
        table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        root.getChildren().addAll(title, toolbar, summary, table);
        loadReport();
        return root;
    }

    @SuppressWarnings("unchecked")
    private TableView<DailyReport.StudentStatus> buildTable() {
        TableColumn<DailyReport.StudentStatus, Integer> rollCol = new TableColumn<>("Roll No");
        rollCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().rollNo()));
        rollCol.setPrefWidth(100);

        TableColumn<DailyReport.StudentStatus, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().name()));
        nameCol.setPrefWidth(250);

        TableColumn<DailyReport.StudentStatus, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().status()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label chip = new Label(item);
                String color = switch (item) {
                    case "PRESENT"    -> "#2ECC71";
                    case "ABSENT"     -> "#FF4D6D";
                    default           -> "#FFC857";
                };
                chip.setStyle(String.format(
                        "-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: %s; " +
                        "-fx-background-radius: 20; -fx-border-color: %s; -fx-border-radius: 20; " +
                        "-fx-padding: 2 10; -fx-font-weight: bold; -fx-font-size: 11px;", color, color));
                setGraphic(chip);
            }
        });
        statusCol.setPrefWidth(140);

        TableView<DailyReport.StudentStatus> tv = new TableView<>();
        tv.getColumns().addAll(rollCol, nameCol, statusCol);
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tv.setStyle("-fx-background-color: #121929; -fx-background-radius: 16; " +
                "-fx-border-color: rgba(108,92,231,0.2); -fx-border-radius: 16;");
        tv.setPlaceholder(new Label("No data for this date"));
        return tv;
    }

    private void loadReport() {
        LocalDate date = datePicker.getValue();
        Task<DailyReport> task = new Task<>() {
            @Override protected DailyReport call() throws Exception {
                return service.getDailyReport(date);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            DailyReport report = task.getValue();
            table.setItems(FXCollections.observableArrayList(report.students()));
            presentLbl.setText("Present: " + report.present());
            absentLbl.setText("Absent: " + report.absent());
            notMarkedLbl.setText("Not Marked: " + report.notMarked());
        }));
        task.setOnFailed(e -> Platform.runLater(() ->
                toast.showError("Failed to load daily report")));
        new Thread(task).start();
    }

    private Label chip(String text, String color) {
        Label lbl = new Label(text);
        lbl.setStyle(String.format(
                "-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: %s; " +
                "-fx-background-radius: 20; -fx-border-color: %s; -fx-border-radius: 20; " +
                "-fx-padding: 6 16; -fx-font-weight: bold;", color, color));
        return lbl;
    }
}
