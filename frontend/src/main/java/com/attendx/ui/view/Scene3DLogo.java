package com.attendx.ui.view;

import javafx.animation.*;
import javafx.scene.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 3D hero logo for the Dashboard: rotating "A" shape built from boxes
 * in Sage Green & Cream White + a particle star-field of drifting spheres.
 */
public class Scene3DLogo {

    private static final int PARTICLE_COUNT = 120;

    public SubScene build() {
        Group world = new Group();

        // ── Build letter "A" from boxes ─────────────────────────
        PhongMaterial logoMat = new PhongMaterial();
        logoMat.setDiffuseColor(Color.web("#4D735E"));
        logoMat.setSpecularColor(Color.web("#E8F0EC"));
        logoMat.setSpecularPower(70);

        PhongMaterial accentMat = new PhongMaterial();
        accentMat.setDiffuseColor(Color.web("#759784"));
        accentMat.setSpecularColor(Color.WHITE);
        accentMat.setSpecularPower(80);

        // Left leg
        Box leftLeg = box(8, 60, 8, logoMat, -16, 10, 0);
        // Right leg
        Box rightLeg = box(8, 60, 8, logoMat, 16, 10, 0);
        // Cross bar
        Box crossBar = box(32, 8, 8, accentMat, 0, -6, 0);
        // Peak
        Box peak = box(8, 24, 8, logoMat, 0, -40, 0);

        // Apply shared rotation to the logo group
        Group logoGroup = new Group(leftLeg, rightLeg, crossBar, peak);

        Rotate rotY = new Rotate(0, Rotate.Y_AXIS);
        logoGroup.getTransforms().addAll(rotY);

        Timeline logoSpin = new Timeline(
                new KeyFrame(Duration.ZERO,     new KeyValue(rotY.angleProperty(), 0)),
                new KeyFrame(Duration.seconds(8), new KeyValue(rotY.angleProperty(), 360, Interpolator.LINEAR)));
        logoSpin.setCycleCount(Timeline.INDEFINITE);
        logoSpin.play();

        // ── Particle star-field ──────────────────────────────────
        Random rng = new Random(42);
        List<Sphere> particles = new ArrayList<>();
        List<Timeline> particleTimelines = new ArrayList<>();

        PhongMaterial starMat = new PhongMaterial(Color.web("#8CA999", 0.7));

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            double r = rng.nextDouble() * 0.8 + 0.3;
            Sphere s = new Sphere(r);
            s.setMaterial(i % 5 == 0 ? accentMat : starMat);

            double x = (rng.nextDouble() - 0.5) * 400;
            double y = (rng.nextDouble() - 0.5) * 160;
            double z = (rng.nextDouble() - 0.5) * 200;
            s.setTranslateX(x); s.setTranslateY(y); s.setTranslateZ(z);

            Timeline drift = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(s.translateXProperty(), x),
                    new KeyValue(s.translateYProperty(), y)),
                new KeyFrame(Duration.seconds(5 + rng.nextDouble() * 10),
                    new KeyValue(s.translateXProperty(), x + (rng.nextDouble()-0.5)*60, Interpolator.EASE_BOTH),
                    new KeyValue(s.translateYProperty(), y + (rng.nextDouble()-0.5)*40, Interpolator.EASE_BOTH)));
            drift.setCycleCount(Timeline.INDEFINITE);
            drift.setAutoReverse(true);
            drift.setDelay(Duration.millis(rng.nextInt(3000)));
            drift.play();

            particles.add(s);
            particleTimelines.add(drift);
        }

        world.getChildren().addAll(particles);
        world.getChildren().add(logoGroup);

        // ── Lighting ─────────────────────────────────────────────
        AmbientLight ambient = new AmbientLight(Color.web("#F4F0E8"));
        PointLight point1 = new PointLight(Color.web("#759784"));
        point1.setTranslateX(-100); point1.setTranslateY(-80); point1.setTranslateZ(-100);
        PointLight point2 = new PointLight(Color.web("#FFFFFF", 0.8));
        point2.setTranslateX(100); point2.setTranslateY(60); point2.setTranslateZ(80);
        world.getChildren().addAll(ambient, point1, point2);

        // ── Camera ───────────────────────────────────────────────
        PerspectiveCamera cam = new PerspectiveCamera(true);
        cam.setTranslateZ(-220);
        cam.setNearClip(0.1);
        cam.setFarClip(2000);
        cam.setFieldOfView(45);

        SubScene scene3D = new SubScene(world, 480, 200, true, SceneAntialiasing.BALANCED);
        scene3D.setFill(Color.TRANSPARENT);
        scene3D.setCamera(cam);

        // Mouse parallax
        scene3D.setOnMouseMoved(e -> {
            double dx = (e.getX() - 240) / 240;
            double dy = (e.getY() - 100) / 100;
            cam.setTranslateX(dx * 20);
            cam.setTranslateY(dy * 10);
        });

        return scene3D;
    }

    private Box box(double w, double h, double d, PhongMaterial mat, double tx, double ty, double tz) {
        Box b = new Box(w, h, d);
        b.setMaterial(mat);
        b.setTranslateX(tx); b.setTranslateY(ty); b.setTranslateZ(tz);
        return b;
    }
}
