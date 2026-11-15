package com.attendx.ui.controller;

import com.attendx.ui.model.Student;
import com.attendx.ui.service.AttendXService;
import com.attendx.ui.util.AnimationUtil;
import com.attendx.ui.util.ToastManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class StudentsController {

    private final AttendXService service;
    private final ToastManager toast;
    private TableView<Student> table;
    private final ObservableList<Student> students = FXCollections.observableArrayList();
    private TextField searchField;
    private Label totalBadge;

    public StudentsController(AttendXService service, ToastManager toast) {
        this.service = service;
        this.toast = toast;
    }

    public Node buildView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #FAF8F5;");

        // Header
        Label title = new Label("Student Directory");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #1B2B24;");

        totalBadge = new Label("0 Students");
        totalBadge.setStyle("-fx-background-color: #E2F1E8; -fx-text-fill: #2D6B49; " +
                "-fx-font-weight: bold; -fx-padding: 5 14; -fx-background-radius: 12; -fx-font-size: 12px; " +
                "-fx-border-color: #A3D1B5; -fx-border-radius: 12;");

        HBox headerRow = new HBox(12, title, totalBadge);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        // Toolbar
        searchField = new TextField();
        searchField.setPromptText("🔍  Search by student name or roll number...");
        searchField.setPrefWidth(340);
        searchField.setStyle(inputStyle());
        searchField.textProperty().addListener((obs, old, val) -> filterStudents(val));

        Button addBtn = primaryButton("+ Add New Student");
        addBtn.setOnAction(e -> showAddDialog());

        HBox toolbar = new HBox(14, searchField, new Region(), addBtn);
        HBox.setHgrow(toolbar.getChildren().get(1), Priority.ALWAYS);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // Table
        table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        root.getChildren().addAll(headerRow, toolbar, table);
        loadStudents();
        return root;
    }

    @SuppressWarnings("unchecked")
    private TableView<Student> buildTable() {
        TableColumn<Student, Integer> rollCol = new TableColumn<>("Roll No");
        rollCol.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().rollNo()));
        rollCol.setPrefWidth(110);
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

        TableColumn<Student, String> nameCol = new TableColumn<>("Student Name");
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().name()));
        nameCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                String initials = item.length() > 0 ? item.substring(0, 1).toUpperCase() : "?";
                Label avatar = new Label(initials);
                avatar.setStyle("-fx-background-color: #E2ECE5; -fx-text-fill: #3D5E4C; " +
                        "-fx-font-weight: bold; -fx-background-radius: 16; -fx-min-width: 32; -fx-min-height: 32; " +
                        "-fx-alignment: center; -fx-font-size: 13px; -fx-border-color: #CCD6CF; -fx-border-radius: 16;");
                Label nameLbl = new Label(item);
                nameLbl.setStyle("-fx-text-fill: #1B2B24; -fx-font-weight: bold; -fx-font-size: 14px;");
                HBox box = new HBox(12, avatar, nameLbl);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        });
        nameCol.setPrefWidth(300);

        TableColumn<Student, Void> actionsCol = new TableColumn<>("Action");
        actionsCol.setCellFactory(col -> new TableCell<>() {
            final Button del = new Button("Delete");
            {
                del.setStyle("-fx-background-color: #FDF0F0; -fx-text-fill: #BA4E4E; " +
                        "-fx-border-color: #E5A3A3; -fx-border-radius: 8; -fx-background-radius: 8; " +
                        "-fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 5 14;");
                del.setOnAction(e -> {
                    Student s = getTableView().getItems().get(getIndex());
                    showDeleteConfirm(s);
                });
                del.setOnMouseEntered(ev -> del.setStyle("-fx-background-color: #BA4E4E; -fx-text-fill: white; " +
                        "-fx-border-color: #BA4E4E; -fx-border-radius: 8; -fx-background-radius: 8; " +
                        "-fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 5 14;"));
                del.setOnMouseExited(ev -> del.setStyle("-fx-background-color: #FDF0F0; -fx-text-fill: #BA4E4E; " +
                        "-fx-border-color: #E5A3A3; -fx-border-radius: 8; -fx-background-radius: 8; " +
                        "-fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 5 14;"));
                AnimationUtil.addHoverScale(del, 1.04);
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : del);
                if (!empty) setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        });
        actionsCol.setPrefWidth(120);
        actionsCol.setStyle("-fx-alignment: CENTER;");

        TableView<Student> tv = new TableView<>(students);
        tv.getColumns().addAll(rollCol, nameCol, actionsCol);
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tv.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 14; " +
                "-fx-border-color: #E3DED6; -fx-border-radius: 14; " +
                "-fx-effect: dropshadow(gaussian, rgba(39,60,51,0.06), 14, 0, 0, 3);");
        Label placeholder = new Label("No students enrolled yet");
        placeholder.setStyle("-fx-text-fill: #5E7267; -fx-font-size: 14px;");
        tv.setPlaceholder(placeholder);
        return tv;
    }

    private void filterStudents(String query) {
        if (query == null || query.isBlank()) {
            table.setItems(students);
            return;
        }
        String lower = query.toLowerCase().trim();
        ObservableList<Student> filtered = FXCollections.observableArrayList(
                students.filtered(s ->
                        s.name().toLowerCase().contains(lower) ||
                        String.valueOf(s.rollNo()).contains(lower)));
        table.setItems(filtered);
    }

    private void loadStudents() {
        Task<List<Student>> task = new Task<>() {
            @Override
            protected List<Student> call() throws Exception {
                return service.listStudents();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            students.setAll(task.getValue());
            totalBadge.setText(students.size() + (students.size() == 1 ? " Student" : " Students"));
        }));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex != null) ex.printStackTrace();
            Platform.runLater(() -> toast.showError("Failed to load students: " + (ex != null ? ex.getMessage() : "")));
        });
        new Thread(task).start();
    }

    private void showAddDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Enroll New Student");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(16);
        grid.setPadding(new Insets(24));

        TextField rollField = new TextField();
        rollField.setPromptText("e.g. 101");
        rollField.setStyle(inputStyle());

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Alice Smith");
        nameField.setStyle(inputStyle());

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #BA4E4E; -fx-font-size: 12px; -fx-font-weight: bold;");
        errorLabel.setVisible(false);

        Label rLbl = new Label("Roll Number:");
        rLbl.setStyle("-fx-text-fill: #1B2B24; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label nLbl = new Label("Full Name:");
        nLbl.setStyle("-fx-text-fill: #1B2B24; -fx-font-weight: bold; -fx-font-size: 13px;");

        grid.add(rLbl, 0, 0);
        grid.add(rollField, 1, 0);
        grid.add(nLbl, 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(errorLabel, 0, 2, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setStyle("-fx-background-color: #FAF8F5; -fx-border-color: #CCD6CF; -fx-border-radius: 14; -fx-background-radius: 14;");

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setStyle("-fx-background-color: #4D735E; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 8 18;");
        okBtn.setText("Save Student");

        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelBtn != null) {
            cancelBtn.setStyle("-fx-background-color: #EFECE5; -fx-text-fill: #1B2B24; -fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 8 18;");
        }

        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String rollStr = rollField.getText().trim();
            String name    = nameField.getText().trim();

            if (rollStr.isBlank() || name.isBlank()) {
                errorLabel.setText("Both Roll Number and Name are required.");
                errorLabel.setVisible(true);
                AnimationUtil.shake(grid);
                event.consume();
                return;
            }
            int roll;
            try {
                roll = Integer.parseInt(rollStr);
            } catch (NumberFormatException ex) {
                errorLabel.setText("Roll number must be an integer.");
                errorLabel.setVisible(true);
                AnimationUtil.shake(grid);
                event.consume();
                return;
            }
            if (roll <= 0) {
                errorLabel.setText("Roll number must be a positive integer (> 0).");
                errorLabel.setVisible(true);
                AnimationUtil.shake(grid);
                event.consume();
                return;
            }

            try {
                service.addStudent(roll, name);
                toast.showSuccess("Added student: " + name + " (Roll #" + roll + ")");
                loadStudents();
            } catch (AttendXService.DuplicateRollException ex) {
                errorLabel.setText(ex.getMessage());
                errorLabel.setVisible(true);
                AnimationUtil.shake(grid);
                event.consume();
            } catch (Exception ex) {
                errorLabel.setText("Error: " + ex.getMessage());
                errorLabel.setVisible(true);
                AnimationUtil.shake(grid);
                event.consume();
            }
        });

        dialog.showAndWait();
    }

    private void showDeleteConfirm(Student s) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete student " + s.name() + " (Roll #" + s.rollNo() + ")?\nAll associated attendance records will be permanently removed.",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText(null);
        alert.getDialogPane().setStyle("-fx-background-color: #FAF8F5; -fx-border-color: #CCD6CF; -fx-border-radius: 12;");
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        service.deleteStudent(s.rollNo());
                        return null;
                    }
                };
                task.setOnSucceeded(e -> Platform.runLater(() -> {
                    toast.showSuccess("Deleted student: " + s.name());
                    loadStudents();
                }));
                task.setOnFailed(e -> {
                    Throwable ex = task.getException();
                    if (ex != null) ex.printStackTrace();
                    Platform.runLater(() -> toast.showError("Delete failed: " + (ex != null ? ex.getMessage() : "")));
                });
                new Thread(task).start();
            }
        });
    }

    private Button primaryButton(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: #4D735E; -fx-text-fill: white; " +
                "-fx-background-radius: 10; -fx-padding: 10 22; -fx-font-weight: bold; -fx-cursor: hand;");
        AnimationUtil.addHoverScale(btn, 1.05);
        return btn;
    }

    private String inputStyle() {
        return "-fx-background-color: #FFFFFF; -fx-text-fill: #1B2B24; " +
                "-fx-prompt-text-fill: #8E9F95; " +
                "-fx-border-color: #CCD6CF; -fx-border-radius: 8; " +
                "-fx-background-radius: 8; -fx-padding: 8 12;";
    }
}
