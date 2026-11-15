package com.attendx.ui.util;

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.Deque;

public class ToastManager {

    private final Pane overlay;
    private final Deque<HBox> toasts = new ArrayDeque<>();

    public ToastManager(Pane overlay) {
        this.overlay = overlay;
        overlay.setPickOnBounds(false);
    }

    public void showSuccess(String message) {
        show(message, "#36825B");
    }

    public void showError(String message) {
        show(message, "#BA4E4E");
    }

    public void showInfo(String message) {
        show(message, "#4D735E");
    }

    private void show(String message, String color) {
        HBox toast = buildToast(message, color);
        overlay.getChildren().add(toast);

        toast.setTranslateX(overlay.getWidth() - 320 - 20);
        double yPos = 20 + (toasts.size() * 60);
        toast.setTranslateY(yPos - 50);
        toasts.addLast(toast);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), toast);
        slideIn.setToY(yPos);
        slideIn.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        ParallelTransition enter = new ParallelTransition(slideIn, fadeIn);
        enter.play();

        PauseTransition pause = new PauseTransition(Duration.seconds(3.2));
        pause.setOnFinished(e -> dismiss(toast));
        pause.play();
    }

    private void dismiss(HBox toast) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), toast);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            overlay.getChildren().remove(toast);
            toasts.remove(toast);
        });
        fadeOut.play();
    }

    private HBox buildToast(String message, String color) {
        Label lbl = new Label(message);
        lbl.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");

        HBox box = new HBox(lbl);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setSpacing(10);
        box.setPrefWidth(320);
        box.setPrefHeight(48);
        box.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: 12; " +
                "-fx-padding: 10 18; -fx-effect: dropshadow(gaussian, rgba(39,60,51,0.22), 14, 0, 0, 4);",
                color));
        box.setOpacity(0);
        return box;
    }
}
