package com.attendx.ui.view;

import com.attendx.ui.controller.*;
import com.attendx.ui.service.ApiClient;
import com.attendx.ui.service.AttendXService;
import com.attendx.ui.util.AnimationUtil;
import com.attendx.ui.util.ToastManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;

public class MainWindow {

    private AttendXService service;
    private ToastManager toast;
    private StackPane contentArea;
    private Node currentContent;
    private VBox sidebarItems;
    private Rectangle activeIndicator;
    private int activeNavIndex = 0;
    private Label offlineBanner;
    private boolean isOnline = true;

    // Screen controllers
    private DashboardController dashboardCtrl;
    private StudentsController studentsCtrl;
    private MarkAttendanceController markAttendanceCtrl;
    private DailyReportController dailyReportCtrl;
    private PercentageController percentageCtrl;

    public void show(Stage stage) {
        ApiClient apiClient = new ApiClient();
        service = new AttendXService(apiClient);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0B1020;");

        // Build offline banner
        offlineBanner = buildOfflineBanner();
        offlineBanner.setVisible(false);

        VBox sidebar = buildSidebar();
        root.setLeft(sidebar);

        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #0B1020;");

        VBox mainArea = new VBox();
        mainArea.getChildren().addAll(offlineBanner, contentArea);
        VBox.setVgrow(contentArea, Priority.ALWAYS);
        root.setCenter(mainArea);

        // Toast overlay
        StackPane overlay = new StackPane();
        overlay.setPickOnBounds(false);
        overlay.setMouseTransparent(true);
        toast = new ToastManager(overlay);

        StackPane outerStack = new StackPane(root, overlay);

        Scene scene = new Scene(outerStack, 1280, 800);
        scene.getStylesheets().add(
                getClass().getResource("/css/attendx.css").toExternalForm());

        stage.setTitle("AttendX — Student Attendance Management");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(680);
        stage.show();

        // Build controllers
        dashboardCtrl     = new DashboardController(service, toast);
        studentsCtrl      = new StudentsController(service, toast);
        markAttendanceCtrl = new MarkAttendanceController(service, toast);
        dailyReportCtrl   = new DailyReportController(service, toast);
        percentageCtrl    = new PercentageController(service, toast);

        // Show dashboard on start
        navigateTo(0);

        // Start periodic health check
        startHealthCheck();
    }

    // ── Sidebar ──────────────────────────────────────────────────
    private VBox buildSidebar() {
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: #0D1428; -fx-padding: 0;");
        sidebar.setSpacing(0);

        // Logo area
        Label logoLabel = new Label("AttendX");
        logoLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; " +
                "-fx-text-fill: #6C5CE7; -fx-padding: 24 20 20 20;");

        Label versionLabel = new Label("v1.0  —  Attendance Mgr");
        versionLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #5a6a8a; -fx-padding: 0 20 20 20;");

        // Nav items
        String[] icons  = {"⊞", "👤", "✓", "📊", "📈"};
        String[] labels = {"Dashboard", "Students", "Mark Attendance", "Daily Report", "Percentage"};
        sidebarItems = new VBox();
        sidebarItems.setSpacing(2);
        sidebarItems.setPadding(new Insets(8, 8, 8, 8));

        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            HBox item = buildNavItem(icons[i], labels[i]);
            item.setOnMouseClicked(e -> navigateTo(idx));
            sidebarItems.getChildren().add(item);
            AnimationUtil.addHoverScale(item, 1.02);
        }

        // Active indicator bar
        activeIndicator = new Rectangle(4, 44, Color.web("#6C5CE7"));
        activeIndicator.setArcWidth(4); activeIndicator.setArcHeight(4);
        activeIndicator.setVisible(false); // overlaid via CSS instead

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Settings at bottom
        HBox settingsItem = buildNavItem("⚙", "Settings");
        settingsItem.setOnMouseClicked(e -> showSettingsPopup());

        sidebar.getChildren().addAll(
                logoLabel, versionLabel, sidebarItems, spacer, settingsItem);
        return sidebar;
    }

    private HBox buildNavItem(String icon, String label) {
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #8899bb;");
        iconLabel.setPrefWidth(28);

        Label textLabel = new Label(label);
        textLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #8899bb;");

        HBox item = new HBox(10, iconLabel, textLabel);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(12, 16, 12, 16));
        item.setStyle("-fx-background-radius: 10; -fx-cursor: hand;");
        item.getProperties().put("iconLabel", iconLabel);
        item.getProperties().put("textLabel", textLabel);
        return item;
    }

    private void navigateTo(int idx) {
        // Update sidebar active state
        List<Node> items = sidebarItems.getChildren();
        for (int i = 0; i < items.size(); i++) {
            HBox item = (HBox) items.get(i);
            Label icon = (Label) item.getProperties().get("iconLabel");
            Label text = (Label) item.getProperties().get("textLabel");
            if (i == idx) {
                item.setStyle("-fx-background-color: rgba(108,92,231,0.2); " +
                        "-fx-background-radius: 10; -fx-cursor: hand;");
                icon.setStyle("-fx-font-size: 16px; -fx-text-fill: #6C5CE7;");
                text.setStyle("-fx-font-size: 13px; -fx-text-fill: #FFFFFF; -fx-font-weight: bold;");
            } else {
                item.setStyle("-fx-background-radius: 10; -fx-cursor: hand;");
                icon.setStyle("-fx-font-size: 16px; -fx-text-fill: #8899bb;");
                text.setStyle("-fx-font-size: 13px; -fx-text-fill: #8899bb;");
            }
        }
        activeNavIndex = idx;
        loadScreen(idx);
    }

    private void loadScreen(int idx) {
        Node newContent = switch (idx) {
            case 0 -> dashboardCtrl.buildView();
            case 1 -> studentsCtrl.buildView();
            case 2 -> markAttendanceCtrl.buildView();
            case 3 -> dailyReportCtrl.buildView();
            case 4 -> percentageCtrl.buildView();
            default -> new Label("Unknown screen");
        };

        if (currentContent == null) {
            contentArea.getChildren().setAll(newContent);
            currentContent = newContent;
            AnimationUtil.fadeSlideIn(newContent, 20, 0);
        } else {
            Node oldContent = currentContent;
            AnimationUtil.pageTransition(oldContent, newContent, () -> {
                contentArea.getChildren().setAll(newContent);
                currentContent = newContent;
            });
        }
    }

    private Label buildOfflineBanner() {
        Label banner = new Label(
                "⚠  Backend offline — cannot reach " +
                System.getProperty("attendx.api", "http://localhost:8080/api/v1") +
                "   Click to retry");
        banner.setStyle("-fx-background-color: #FF4D6D; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-padding: 10; -fx-cursor: hand;");
        banner.setMaxWidth(Double.MAX_VALUE);
        banner.setAlignment(Pos.CENTER);
        banner.setOnMouseClicked(e -> checkOnline());
        return banner;
    }

    private void startHealthCheck() {
        // Check every 10 seconds
        Timeline tl = new Timeline(new KeyFrame(Duration.seconds(10),
                e -> checkOnline()));
        tl.setCycleCount(Timeline.INDEFINITE);
        tl.play();
        checkOnline();
    }

    private void checkOnline() {
        boolean online = service.isBackendOnline();
        Platform.runLater(() -> {
            if (online && !isOnline) {
                offlineBanner.setVisible(false);
                toast.showSuccess("Backend reconnected!");
                isOnline = true;
                navigateTo(activeNavIndex); // refresh
            } else if (!online && isOnline) {
                offlineBanner.setVisible(true);
                isOnline = false;
            }
        });
    }

    private void showSettingsPopup() {
        toast.showInfo("Settings: Use -Dattendx.reduceMotion=true to disable animations");
    }
}
