package com.attendx.ui.util;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.Duration;

public class ToastManager {

    private final VBox container;

    public ToastManager(Pane overlay) {
        container = new VBox(8);
        container.setPickOnBounds(false);
        container.setAlignment(Pos.TOP_RIGHT);
        container.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        if (overlay instanceof StackPane) {
            StackPane.setAlignment(container, Pos.TOP_RIGHT);
            StackPane.setMargin(container, new Insets(16, 20, 0, 0));
        }

        overlay.getChildren().add(container);
    }

    public void showSuccess(String message) {
        show("✓", message, "#36825B");
    }

    public void showError(String message) {
        show("✕", message, "#BA4E4E");
    }

    public void showInfo(String message) {
        show("ℹ", message, "#4D735E");
    }

    private void show(String icon, String message, String color) {
        Platform.runLater(() -> {
            HBox toast = buildToast(icon, message, color);
            container.getChildren().add(toast);

            // Subtle slide-in from right + fade-in
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(220), toast);
            slideIn.setFromX(40);
            slideIn.setToX(0);
            slideIn.setInterpolator(Interpolator.EASE_OUT);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(220), toast);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            ParallelTransition enter = new ParallelTransition(slideIn, fadeIn);
            enter.play();

            // Auto-dismiss after 2.8 seconds
            PauseTransition pause = new PauseTransition(Duration.seconds(2.8));
            pause.setOnFinished(e -> dismiss(toast));
            pause.play();
        });
    }

    private void dismiss(HBox toast) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), toast);
        fadeOut.setToValue(0);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(200), toast);
        slideOut.setToX(40);
        slideOut.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition exit = new ParallelTransition(fadeOut, slideOut);
        exit.setOnFinished(e -> container.getChildren().remove(toast));
        exit.play();
    }

    private HBox buildToast(String icon, String message, String color) {
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label msgLbl = new Label(message);
        msgLbl.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
        msgLbl.setWrapText(true);
        msgLbl.setMaxWidth(260);

        HBox box = new HBox(8, iconLbl, msgLbl);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(8, 14, 8, 12));
        box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        box.setMinHeight(36);
        box.setMaxWidth(300);
        box.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: 8; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 8, 0, 0, 2);",
                color));
        box.setOpacity(0);
        return box;
    }
}
