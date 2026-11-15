package com.attendx.ui;

import com.attendx.ui.view.MainWindow;
import javafx.application.Application;
import javafx.stage.Stage;

public class AttendXApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        MainWindow window = new MainWindow();
        window.show(primaryStage);
    }
}
