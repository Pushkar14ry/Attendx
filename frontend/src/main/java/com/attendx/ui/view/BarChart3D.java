package com.attendx.ui.view;

import com.attendx.ui.model.AttendanceSummary;
import javafx.animation.*;
import javafx.scene.*;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.*;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.util.List;

/**
 * Interactive 3D bar chart styled in Sage Green & Cream White.
 * - Bars grow from zero on load.
 * - Mouse drag orbits the chart.
 * - Scroll zooms.
 * - Hover highlights a bar.
 */
public class BarChart3D {

    private final Group barsGroup = new Group();
    private final Rotate rotateX  = new Rotate(-20, Rotate.X_AXIS);
    private final Rotate rotateY  = new Rotate(30,  Rotate.Y_AXIS);
    private double lastMouseX, lastMouseY;
    private SubScene scene3D;
    private PerspectiveCamera camera;

    public Node build() {
        Group world = new Group();

        // Floor plane: Warm Linen
        Box floor = new Box(400, 2, 300);
        PhongMaterial floorMat = new PhongMaterial();
        floorMat.setDiffuseColor(Color.web("#ECE7DE"));
        floorMat.setSpecularColor(Color.web("#4D735E", 0.25));
        floorMat.setSpecularPower(40);
        floor.setMaterial(floorMat);
        floor.setTranslateY(60);

        // Grid lines: Soft Sage
        for (int i = -150; i <= 150; i += 30) {
            Box line = new Box(400, 0.6, 0.6);
            line.setTranslateY(59);
            line.setTranslateZ(i);
            PhongMaterial lineMat = new PhongMaterial(Color.web("#759784", 0.3));
            line.setMaterial(lineMat);
            world.getChildren().add(line);
        }

        barsGroup.getTransforms().addAll(rotateX, rotateY);
        world.getChildren().addAll(floor, barsGroup);

        // Lighting: Warm natural light
        AmbientLight ambient = new AmbientLight(Color.web("#FAF8F5"));
        PointLight light1 = new PointLight(Color.web("#759784"));
        light1.setTranslateX(-200); light1.setTranslateY(-200); light1.setTranslateZ(-200);
        PointLight light2 = new PointLight(Color.web("#FFFFFF", 0.75));
        light2.setTranslateX(150); light2.setTranslateY(-100); light2.setTranslateZ(100);
        world.getChildren().addAll(ambient, light1, light2);

        camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-500);
        camera.setNearClip(1);
        camera.setFarClip(5000);
        camera.setFieldOfView(40);

        scene3D = new SubScene(world, 700, 300, true, SceneAntialiasing.BALANCED);
        scene3D.setFill(Color.TRANSPARENT);
        scene3D.setCamera(camera);

        // Mouse orbit
        scene3D.setOnMousePressed(e -> { lastMouseX = e.getX(); lastMouseY = e.getY(); });
        scene3D.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                double dx = e.getX() - lastMouseX;
                double dy = e.getY() - lastMouseY;
                rotateY.setAngle(rotateY.getAngle() + dx * 0.5);
                rotateX.setAngle(Math.max(-60, Math.min(10, rotateX.getAngle() + dy * 0.3)));
                lastMouseX = e.getX(); lastMouseY = e.getY();
            }
        });
        scene3D.setOnScroll(e -> {
            double z = camera.getTranslateZ() + e.getDeltaY() * 1.2;
            camera.setTranslateZ(Math.max(-900, Math.min(-150, z)));
        });

        return scene3D;
    }

    public void updateData(List<AttendanceSummary.DateTrend> trend) {
        barsGroup.getChildren().clear();
        if (trend == null || trend.isEmpty()) return;

        int count = Math.min(trend.size(), 12);
        List<AttendanceSummary.DateTrend> visible = trend.subList(
                Math.max(0, trend.size() - count), trend.size());

        int maxTotal = visible.stream().mapToInt(AttendanceSummary.DateTrend::total).max().orElse(1);
        double maxBarHeight = 110.0;
        double barWidth = 24;
        double spacing = 38;
        double startX = -(visible.size() - 1) * spacing / 2.0;

        PhongMaterial presentMat = glossMaterial("#36825B");
        PhongMaterial absentMat  = glossMaterial("#BA4E4E");

        for (int i = 0; i < visible.size(); i++) {
            AttendanceSummary.DateTrend d = visible.get(i);
            double x = startX + i * spacing;
            double targetPresent = maxTotal > 0 ? (double) d.present() / maxTotal * maxBarHeight : 2;
            double targetAbsent  = maxTotal > 0 ? (double) d.absent()  / maxTotal * maxBarHeight * 0.8 : 2;

            // Present bar (Leaf Green)
            Box pBar = new Box(barWidth, 1, barWidth * 0.8);
            pBar.setMaterial(presentMat);
            pBar.setTranslateX(x - barWidth * 0.35);
            pBar.setTranslateY(60);
            barsGroup.getChildren().add(pBar);

            // Absent bar (Terracotta)
            Box aBar = new Box(barWidth * 0.8, 1, barWidth * 0.8);
            aBar.setMaterial(absentMat);
            aBar.setTranslateX(x + barWidth * 0.45);
            aBar.setTranslateY(60);
            barsGroup.getChildren().add(aBar);

            // Animate bars growing
            final double fTargetP = Math.max(4, targetPresent);
            final double fTargetA = Math.max(4, targetAbsent);
            Timeline grow = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(pBar.heightProperty(), 1),
                    new KeyValue(aBar.heightProperty(), 1),
                    new KeyValue(pBar.translateYProperty(), 60),
                    new KeyValue(aBar.translateYProperty(), 60)),
                new KeyFrame(Duration.millis(600 + i * 80),
                    new KeyValue(pBar.heightProperty(), fTargetP, Interpolator.EASE_OUT),
                    new KeyValue(aBar.heightProperty(), fTargetA, Interpolator.EASE_OUT),
                    new KeyValue(pBar.translateYProperty(), 60 - fTargetP / 2, Interpolator.EASE_OUT),
                    new KeyValue(aBar.translateYProperty(), 60 - fTargetA / 2, Interpolator.EASE_OUT)));
            grow.play();

            // Hover highlight
            pBar.setOnMouseEntered(e -> {
                pBar.setMaterial(glossMaterial("#48A876"));
                pBar.setTranslateY(pBar.getTranslateY() - 4);
            });
            pBar.setOnMouseExited(e -> {
                pBar.setMaterial(presentMat);
                pBar.setTranslateY(pBar.getTranslateY() + 4);
            });
            aBar.setOnMouseEntered(e -> {
                aBar.setMaterial(glossMaterial("#D66B6B"));
                aBar.setTranslateY(aBar.getTranslateY() - 4);
            });
            aBar.setOnMouseExited(e -> {
                aBar.setMaterial(absentMat);
                aBar.setTranslateY(aBar.getTranslateY() + 4);
            });
        }
    }

    /** Update chart with 3 categorical bars for daily report (Present, Absent, Not Marked) */
    public void updateDailyData(int present, int absent, int notMarked) {
        barsGroup.getChildren().clear();
        int total = Math.max(1, present + absent + notMarked);
        double maxBarHeight = 110.0;
        double barWidth = 36;
        double spacing = 60;

        int[] counts = {present, absent, notMarked};
        String[] colors = {"#36825B", "#BA4E4E", "#D49B45"};
        String[] hoverColors = {"#48A876", "#D66B6B", "#E5B263"};

        for (int i = 0; i < 3; i++) {
            double x = (i - 1) * spacing;
            double targetH = Math.max(6, (double) counts[i] / total * maxBarHeight);

            PhongMaterial mat = glossMaterial(colors[i]);
            PhongMaterial hovMat = glossMaterial(hoverColors[i]);

            Box bar = new Box(barWidth, 1, barWidth);
            bar.setMaterial(mat);
            bar.setTranslateX(x);
            bar.setTranslateY(60);
            barsGroup.getChildren().add(bar);

            final double fTarget = targetH;
            Timeline grow = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(bar.heightProperty(), 1),
                    new KeyValue(bar.translateYProperty(), 60)),
                new KeyFrame(Duration.millis(500 + i * 100),
                    new KeyValue(bar.heightProperty(), fTarget, Interpolator.EASE_OUT),
                    new KeyValue(bar.translateYProperty(), 60 - fTarget / 2, Interpolator.EASE_OUT)));
            grow.play();

            bar.setOnMouseEntered(e -> {
                bar.setMaterial(hovMat);
                bar.setTranslateY(bar.getTranslateY() - 4);
            });
            bar.setOnMouseExited(e -> {
                bar.setMaterial(mat);
                bar.setTranslateY(bar.getTranslateY() + 4);
            });
        }
    }

    private PhongMaterial glossMaterial(String hexColor) {
        PhongMaterial mat = new PhongMaterial();
        mat.setDiffuseColor(Color.web(hexColor));
        mat.setSpecularColor(Color.WHITE);
        mat.setSpecularPower(80);
        return mat;
    }
}
