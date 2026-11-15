package com.attendx.ui.view;

import com.attendx.ui.model.AttendanceSummary;
import javafx.animation.*;
import javafx.scene.*;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.*;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Interactive 3D bar chart.
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

        // Floor plane
        Box floor = new Box(400, 2, 300);
        PhongMaterial floorMat = new PhongMaterial();
        floorMat.setDiffuseColor(Color.web("#1a2340"));
        floorMat.setSpecularColor(Color.web("#6C5CE7", 0.3));
        floorMat.setSpecularPower(30);
        floor.setMaterial(floorMat);
        floor.setTranslateY(60);

        // Grid lines
        for (int i = -150; i <= 150; i += 30) {
            Box line = new Box(400, 0.5, 0.5);
            line.setTranslateY(59); line.setTranslateZ(i);
            PhongMaterial lineMat = new PhongMaterial(Color.web("#6C5CE7", 0.15));
            line.setMaterial(lineMat);
            world.getChildren().add(line);
        }

        barsGroup.getTransforms().addAll(rotateX, rotateY);
        world.getChildren().addAll(floor, barsGroup);

        // Lighting
        AmbientLight ambient = new AmbientLight(Color.web("#222244"));
        PointLight light1 = new PointLight(Color.web("#6C5CE7"));
        light1.setTranslateX(-200); light1.setTranslateY(-200); light1.setTranslateZ(-200);
        PointLight light2 = new PointLight(Color.web("#00D2FF", 0.5));
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

        int count = Math.min(trend.size(), 12); // show last 12
        List<AttendanceSummary.DateTrend> visible = trend.subList(
                Math.max(0, trend.size() - count), trend.size());

        int maxTotal = visible.stream().mapToInt(AttendanceSummary.DateTrend::total).max().orElse(1);
        double maxBarHeight = 100.0;
        double barWidth = 24;
        double spacing = 36;
        double startX = -(visible.size() - 1) * spacing / 2.0;

        PhongMaterial presentMat = glossMaterial("#2ECC71");
        PhongMaterial absentMat  = glossMaterial("#FF4D6D");

        List<Box> barsList = new ArrayList<>();

        for (int i = 0; i < visible.size(); i++) {
            AttendanceSummary.DateTrend d = visible.get(i);
            double x = startX + i * spacing;
            double targetPresent = maxTotal > 0 ? (double) d.present() / maxTotal * maxBarHeight : 0;
            double targetAbsent  = maxTotal > 0 ? (double) d.absent()  / maxTotal * maxBarHeight * 0.6 : 0;

            // Present bar
            Box pBar = new Box(barWidth, 1, barWidth * 0.8);
            pBar.setMaterial(presentMat);
            pBar.setTranslateX(x - barWidth * 0.3);
            pBar.setTranslateY(60); // start at floor
            barsGroup.getChildren().add(pBar);

            // Absent bar
            Box aBar = new Box(barWidth * 0.8, 1, barWidth * 0.8);
            aBar.setMaterial(absentMat);
            aBar.setTranslateX(x + barWidth * 0.4);
            aBar.setTranslateY(60);
            barsGroup.getChildren().add(aBar);

            barsList.add(pBar);
            barsList.add(aBar);

            // Animate bars growing
            final double fTargetP = targetPresent;
            final double fTargetA = targetAbsent;
            Timeline grow = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(pBar.heightProperty(), 1),
                    new KeyValue(aBar.heightProperty(), 1),
                    new KeyValue(pBar.translateYProperty(), 60),
                    new KeyValue(aBar.translateYProperty(), 60)),
                new KeyFrame(Duration.millis(600 + i * 80),
                    new KeyValue(pBar.heightProperty(), Math.max(2, fTargetP), Interpolator.EASE_OUT),
                    new KeyValue(aBar.heightProperty(), Math.max(2, fTargetA), Interpolator.EASE_OUT),
                    new KeyValue(pBar.translateYProperty(), 60 - fTargetP / 2, Interpolator.EASE_OUT),
                    new KeyValue(aBar.translateYProperty(), 60 - fTargetA / 2, Interpolator.EASE_OUT)));
            grow.play();

            // Hover highlight
            int idx = i;
            pBar.setOnMouseEntered(e -> {
                pBar.setMaterial(glossMaterial("#00FF9D"));
                pBar.setTranslateY(pBar.getTranslateY() - 5);
            });
            pBar.setOnMouseExited(e -> {
                pBar.setMaterial(presentMat);
                pBar.setTranslateY(pBar.getTranslateY() + 5);
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
