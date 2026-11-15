package com.attendx.ui.view;

import javafx.animation.*;
import javafx.scene.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

/**
 * 3D Gauge/Donut built from Cylinders sweeping to a percentage,
 * styled in Sage Green & Cream White.
 */
public class Gauge3D {

    private static final int SEGMENTS = 36;
    private final Cylinder[] segments = new Cylinder[SEGMENTS];
    private SubScene scene3D;

    public Node build() {
        Group world = new Group();
        double radius = 70;
        double segRadius = 7;

        PhongMaterial baseMat = new PhongMaterial(Color.web("#D8E4DC"));
        for (int i = 0; i < SEGMENTS; i++) {
            Cylinder seg = new Cylinder(segRadius, 8);
            seg.setMaterial(baseMat);
            double angle = Math.toRadians(i * 360.0 / SEGMENTS);
            seg.setTranslateX(radius * Math.sin(angle));
            seg.setTranslateZ(radius * Math.cos(angle));
            Rotate tilt = new Rotate(90, Rotate.X_AXIS);
            seg.getTransforms().add(tilt);
            segments[i] = seg;
            world.getChildren().add(seg);
        }

        // Center sphere in botanical deep sage
        Sphere center = new Sphere(14);
        PhongMaterial centerMat = new PhongMaterial(Color.web("#273C33"));
        centerMat.setSpecularColor(Color.web("#8CA999", 0.6));
        centerMat.setSpecularPower(50);
        center.setMaterial(centerMat);
        world.getChildren().add(center);

        // Warm ambient & directional lighting
        AmbientLight ambient = new AmbientLight(Color.web("#FAF8F5"));
        PointLight light = new PointLight(Color.web("#759784"));
        light.setTranslateX(-100); light.setTranslateY(-80); light.setTranslateZ(-100);
        world.getChildren().addAll(ambient, light);

        // Slow auto-rotate
        Rotate autoRot = new Rotate(0, Rotate.Y_AXIS);
        world.getTransforms().add(autoRot);
        Timeline spin = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(autoRot.angleProperty(), 0)),
            new KeyFrame(Duration.seconds(12), new KeyValue(autoRot.angleProperty(), 360, Interpolator.LINEAR)));
        spin.setCycleCount(Timeline.INDEFINITE);
        spin.play();

        PerspectiveCamera cam = new PerspectiveCamera(true);
        cam.setTranslateZ(-220);
        cam.setNearClip(1);
        cam.setFarClip(2000);
        cam.setFieldOfView(40);

        scene3D = new SubScene(world, 220, 220, true, SceneAntialiasing.BALANCED);
        scene3D.setFill(Color.TRANSPARENT);
        scene3D.setCamera(cam);

        // Mouse drag orbit
        final double[] lastMouse = {0, 0};
        final Rotate rotY = new Rotate(0, Rotate.Y_AXIS);
        world.getTransforms().add(rotY);
        scene3D.setOnMousePressed(e -> { lastMouse[0] = e.getX(); lastMouse[1] = e.getY(); });
        scene3D.setOnMouseDragged(e -> {
            rotY.setAngle(rotY.getAngle() + (e.getX() - lastMouse[0]) * 0.5);
            lastMouse[0] = e.getX(); lastMouse[1] = e.getY();
        });

        return scene3D;
    }

    /** Animate the gauge to fill up to the given percentage, using a colour. */
    public void animateTo(double percentage, String color) {
        int lit = (int) Math.round(percentage / 100.0 * SEGMENTS);
        PhongMaterial litMat = new PhongMaterial();
        litMat.setDiffuseColor(Color.web(color));
        litMat.setSpecularColor(Color.WHITE);
        litMat.setSpecularPower(60);

        PhongMaterial baseMat = new PhongMaterial(Color.web("#D8E4DC"));

        for (Cylinder seg : segments) seg.setMaterial(baseMat);

        Timeline tl = new Timeline();
        for (int i = 0; i < lit; i++) {
            final int idx = i;
            tl.getKeyFrames().add(new KeyFrame(Duration.millis(i * (800.0 / SEGMENTS)),
                e -> segments[idx].setMaterial(litMat)));
        }
        tl.play();
    }
}
