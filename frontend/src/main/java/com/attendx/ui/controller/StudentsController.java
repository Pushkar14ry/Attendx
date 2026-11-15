package com.attendx.ui.controller;

import com.attendx.ui.model.Student;
import com.attendx.ui.service.AttendXService;
import com.attendx.ui.util.AnimationUtil;
import com.attendx.ui.util.ToastManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;

public class StudentsController {

    private final AttendXService service;
    private final ToastManager toast;
    private TableView<Student> table;
    private final ObservableList<Student> students = FXCollections.observableArrayList();
    private TextField searchField;

    public StudentsController(AttendXService service, ToastManager toast) {
        this.service = service;
        this.toast = toast;
    }

    public Node buildView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #0B1020;");

        // Header
        Label title = new Label("Students");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Toolbar
        searchField = new TextField();
        searchField.setPromptText("🔍  Search by name or roll no...");
        searchField.setPrefWidth(280);
        searchField.setStyle(inputStyle());
        searchField.textProperty().addListener((obs, old, val) -> filterStudents(val));

        Button addBtn = primaryButton("+ Add Student");
        addBtn.setOnAction(e -> showAddDialog());

        HBox toolbar = new HBox(12, searchField, new Region(), addBtn);
        HBox.setHgrow(toolbar.getChildren().get(1), Priority.ALWAYS);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // Table
        table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        root.getChildren().addAll(title, toolbar, table);
        loadStudents();
        return root;
    }

    @SuppressWarnings("unchecked")
    private TableView<Student> buildTable() {
        TableColumn<Student, Integer> rollCol = new TableColumn<>("Roll No");
        rollCol.setCellValueFactory(new PropertyValueFactory<>("rollNo"));
        rollCol.setPrefWidth(100);
        rollCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Student, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(300);

        TableColumn<Student, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellFactory(col -> new TableCell<>() {
            final Button del = new Button("Delete");
            {
                del.setStyle("-fx-background-color: rgba(255,77,109,0.2); -fx-text-fill: #FF4D6D; " +
                        "-fx-border-color: #FF4D6D; -fx-border-radius: 6; -fx-background-radius: 6; " +
                        "-fx-cursor: hand; -fx-font-size: 11px;");
                del.setOnAction(e -> {
                    Student s = getTableView().getItems().get(getIndex());
                    showDeleteConfirm(s);
                });
                AnimationUtil.addHoverScale(del, 1.05);
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : del);
            }
        });
        actionsCol.setPrefWidth(120);

        TableView<Student> tv = new TableView<>(students);
        tv.getColumns().addAll(rollCol, nameCol, actionsCol);
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tv.setStyle("-fx-background-color: #121929; -fx-background-radius: 16; " +
                "-fx-border-color: rgba(108,92,231,0.2); -fx-border-radius: 16;");
        tv.setPlaceholder(new Label("No students found"));
        return tv;
    }

    private void filterStudents(String query) {
        if (query == null || query.isBlank()) {
            table.setItems(students);
            return;
        }
        String lower = query.toLowerCase();
        ObservableList<Student> filtered = FXCollections.observableArrayList(
                students.filtered(s ->
                        s.name().toLowerCase().contains(lower) ||
                        String.valueOf(s.rollNo()).contains(lower)));
        table.setItems(filtered);
    }

    private void loadStudents() {
        Task<List<Student>> task = new Task<>() {
            @Override protected List<Student> call() throws Exception {
                return service.listStudents();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            students.setAll(task.getValue());
            // Stagger row animations
            for (int i = 0; i < table.getItems().size(); i++) {
                // JavaFX table rows animate on render naturally
            }
        }));
        task.setOnFailed(e -> Platform.runLater(() ->
                toast.showError("Failed to load students")));
        new Thread(task).start();
    }

    private void showAddDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Student");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField rollField = new TextField();
        rollField.setPromptText("Roll Number");
        rollField.setStyle(inputStyle());

        TextField nameField = new TextField();
        nameField.setPromptText("Full Name");
        nameField.setStyle(inputStyle());

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #FF4D6D; -fx-font-size: 11px;");
        errorLabel.setVisible(false);

        grid.add(new Label("Roll No:"), 0, 0);
        grid.add(rollField, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(errorLabel, 0, 2, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setStyle("-fx-background-color: #0D1428;");

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setStyle("-fx-background-color: #6C5CE7; -fx-text-fill: white; -fx-background-radius: 8;");
        okBtn.setText("Add Student");
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String rollStr = rollField.getText().trim();
            String name    = nameField.getText().trim();

            if (rollStr.isBlank() || name.isBlank()) {
                errorLabel.setText("All fields are required");
                errorLabel.setVisible(true);
                AnimationUtil.shake(grid);
                event.consume();
                return;
            }
            int roll;
            try { roll = Integer.parseInt(rollStr); }
            catch (NumberFormatException ex) {
                errorLabel.setText("Roll number must be a valid integer");
                errorLabel.setVisible(true);
                AnimationUtil.shake(grid);
                event.consume();
                return;
            }
            if (roll <= 0) {
                errorLabel.setText("Roll number must be positive");
                errorLabel.setVisible(true);
                AnimationUtil.shake(grid);
                event.consume();
                return;
            }

            try {
                service.addStudent(roll, name);
                toast.showSuccess("Student added: " + name);
                loadStudents();
            } catch (AttendXService.DuplicateRollException ex) {
                errorLabel.setText(ex.getMessage());
                errorLabel.setVisible(true);
                AnimationUtil.shake(grid);
                event.consume();
            } catch (Exception ex) {
                toast.showError("Error: " + ex.getMessage());
            }
        });

        dialog.showAndWait();
    }

    private void showDeleteConfirm(Student s) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete student " + s.name() + " (Roll No. " + s.rollNo() + ")?\nAll attendance records will be removed.",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirm Delete");
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                Task<Void> task = new Task<>() {
                    @Override protected Void call() throws Exception {
                        service.deleteStudent(s.rollNo());
                        return null;
                    }
                };
                task.setOnSucceeded(e -> Platform.runLater(() -> {
                    toast.showSuccess("Student deleted");
                    loadStudents();
                }));
                task.setOnFailed(e -> Platform.runLater(() ->
                        toast.showError("Failed to delete student")));
                new Thread(task).start();
            }
        });
    }

    private Button primaryButton(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: #6C5CE7; -fx-text-fill: white; " +
                "-fx-background-radius: 10; -fx-padding: 10 20; -fx-font-weight: bold; -fx-cursor: hand;");
        AnimationUtil.addHoverScale(btn, 1.05);
        return btn;
    }

    private String inputStyle() {
        return "-fx-background-color: #1a2340; -fx-text-fill: white; " +
                "-fx-border-color: rgba(108,92,231,0.4); -fx-border-radius: 8; " +
                "-fx-background-radius: 8; -fx-padding: 8 12;";
    }
}
