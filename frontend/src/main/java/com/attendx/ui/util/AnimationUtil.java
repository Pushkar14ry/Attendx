package com.attendx.ui.util;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.util.Duration;

public class AnimationUtil {

    private AnimationUtil() {}

    /** Fade + slide transition: fade in and slide up into place */
    public static void fadeSlideIn(Node node, double fromY, int delayMs) {
        node.setOpacity(0);
        node.setTranslateY(fromY);

        FadeTransition ft = new FadeTransition(Duration.millis(250), node);
        ft.setFromValue(0); ft.setToValue(1);
        ft.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition tt = new TranslateTransition(Duration.millis(250), node);
        tt.setFromY(fromY); tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition anim = new ParallelTransition(ft, tt);
        anim.setDelay(Duration.millis(delayMs));
        anim.play();
    }

    /** Count-up animation for a stat label */
    public static void countUp(javafx.scene.control.Label label, double from, double to, String suffix) {
        Timeline tl = new Timeline();
        int frames = 40;
        for (int i = 0; i <= frames; i++) {
            final double val = from + (to - from) * i / frames;
            tl.getKeyFrames().add(new KeyFrame(
                    Duration.millis(i * 15),
                    e -> label.setText(String.format("%.0f", val) + suffix)));
        }
        tl.play();
    }

    /** Shake animation for error feedback */
    public static void shake(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), node);
        tt.setFromX(0); tt.setByX(10); tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.setOnFinished(e -> node.setTranslateX(0));
        tt.play();
    }

    /** Scale-pulse on hover */
    public static void addHoverScale(Node node, double scale) {
        node.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(120), node);
            st.setToX(scale); st.setToY(scale);
            st.play();
        });
        node.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(120), node);
            st.setToX(1.0); st.setToY(1.0);
            st.play();
        });
        node.setOnMousePressed(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(80), node);
            st.setToX(0.97); st.setToY(0.97);
            st.play();
        });
        node.setOnMouseReleased(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(80), node);
            st.setToX(scale); st.setToY(scale);
            st.play();
        });
    }

    /** Page transition: fade out old, fade in new */
    public static void pageTransition(Node out, Node in, Runnable onSwitch) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), out);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            onSwitch.run();
            in.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), in);
            fadeIn.setToValue(1);
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(200), in);
            slideIn.setFromY(20); slideIn.setToY(0);
            new ParallelTransition(fadeIn, slideIn).play();
        });
        fadeOut.play();
    }

    public static boolean isReduceMotion() {
        return "true".equalsIgnoreCase(System.getProperty("attendx.reduceMotion"));
    }
}
