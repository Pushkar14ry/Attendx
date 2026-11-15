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
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class MainWindow {

    private AttendXService service;
    private ApiClient apiClient;
    private ToastManager toast;
    private StackPane contentArea;
    private Node currentContent;
    private final List<HBox> navButtons = new ArrayList<>();
    private Rectangle slidingIndicator;
    private int activeNavIndex = 0;
    private HBox offlineBanner;
    private boolean isOnline = true;
    private Stage primaryStage;

    // Screen controllers
    private DashboardController dashboardCtrl;
    private StudentsController studentsCtrl;
    private MarkAttendanceController markAttendanceCtrl;
    private DailyReportController dailyReportCtrl;
    private PercentageController percentageCtrl;

    public void show(Stage stage) {
        this.primaryStage = stage;
        this.apiClient = new ApiClient();
        this.service = new AttendXService(apiClient);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #FAF8F5;");

        // Build offline banner
        offlineBanner = buildOfflineBanner();
        offlineBanner.setVisible(false);
        offlineBanner.setManaged(false);

        VBox sidebar = buildSidebar();
        root.setLeft(sidebar);

        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #FAF8F5;");

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

        stage.setTitle("AttendX — Student Attendance Management System");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(680);
        stage.show();

        // Build controllers
        dashboardCtrl      = new DashboardController(service, toast);
        studentsCtrl       = new StudentsController(service, toast);
        markAttendanceCtrl = new MarkAttendanceController(service, toast);
        dailyReportCtrl    = new DailyReportController(service, toast);
        percentageCtrl     = new PercentageController(service, toast);

        // Show dashboard initially
        navigateTo(0);

        // Start periodic health check
        startHealthCheck();
    }

    // ── Sidebar with Sliding Active Indicator ─────────────────────
    private VBox buildSidebar() {
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(230);
        sidebar.setStyle("-fx-background-color: #273C33;");
        sidebar.setSpacing(0);

        // Logo area
        Label logoLabel = new Label("AttendX");
        logoLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; " +
                "-fx-text-fill: #E8EFEA; -fx-padding: 24 20 4 20;");

        Label versionLabel = new Label("Student Attendance 3D System");
        versionLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9AB2A4; -fx-padding: 0 20 20 20;");

        // Nav items container with sliding indicator
        String[] icons  = {"⊞", "👤", "✓", "📊", "📈"};
        String[] labels = {"Dashboard", "Students", "Mark Attendance", "Daily Report", "Percentage"};

        VBox itemsBox = new VBox();
        itemsBox.setSpacing(6);
        itemsBox.setPadding(new Insets(10, 10, 10, 10));

        // Sliding indicator: 4px wide bar in Sage Mist
        slidingIndicator = new Rectangle(4, 38, Color.web("#759784"));
        slidingIndicator.setArcWidth(4);
        slidingIndicator.setArcHeight(4);

        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            HBox item = buildNavItem(icons[i], labels[i]);
            item.setOnMouseClicked(e -> navigateTo(idx));
            itemsBox.getChildren().add(item);
            navButtons.add(item);
            AnimationUtil.addHoverScale(item, 1.02);
        }

        StackPane navStack = new StackPane();
        navStack.setAlignment(Pos.TOP_LEFT);
        navStack.getChildren().addAll(itemsBox, slidingIndicator);
        slidingIndicator.setTranslateX(4);
        slidingIndicator.setTranslateY(15);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Settings button at bottom
        HBox settingsItem = buildNavItem("⚙", "Settings & Info");
        settingsItem.setOnMouseClicked(e -> showSettingsDialog());
        AnimationUtil.addHoverScale(settingsItem, 1.02);
        VBox.setMargin(settingsItem, new Insets(10, 10, 20, 10));

        sidebar.getChildren().addAll(
                logoLabel, versionLabel, navStack, spacer, settingsItem);
        return sidebar;
    }

    private HBox buildNavItem(String icon, String label) {
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #9AB2A4;");
        iconLabel.setPrefWidth(28);

        Label textLabel = new Label(label);
        textLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #9AB2A4; -fx-font-weight: bold;");

        HBox item = new HBox(12, iconLabel, textLabel);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(10, 16, 10, 16));
        item.setStyle("-fx-background-radius: 10; -fx-cursor: hand;");
        item.getProperties().put("iconLabel", iconLabel);
        item.getProperties().put("textLabel", textLabel);
        return item;
    }

    private void navigateTo(int idx) {
        activeNavIndex = idx;

        // Slide indicator smoothly
        double targetY = 15 + (idx * (38 + 6 + 4));
        if (AnimationUtil.isReduceMotion()) {
            slidingIndicator.setTranslateY(targetY);
        } else {
            TranslateTransition tt = new TranslateTransition(Duration.millis(200), slidingIndicator);
            tt.setToY(targetY);
            tt.setInterpolator(Interpolator.EASE_OUT);
            tt.play();
        }

        // Update items styling
        for (int i = 0; i < navButtons.size(); i++) {
            HBox item = navButtons.get(i);
            Label icon = (Label) item.getProperties().get("iconLabel");
            Label text = (Label) item.getProperties().get("textLabel");
            if (i == idx) {
                item.setStyle("-fx-background-color: rgba(117,151,132,0.28); " +
                        "-fx-background-radius: 10; -fx-cursor: hand;");
                icon.setStyle("-fx-font-size: 16px; -fx-text-fill: #C6E0D0;");
                text.setStyle("-fx-font-size: 13px; -fx-text-fill: #FFFFFF; -fx-font-weight: bold;");
            } else {
                item.setStyle("-fx-background-color: transparent; -fx-background-radius: 10; -fx-cursor: hand;");
                icon.setStyle("-fx-font-size: 16px; -fx-text-fill: #9AB2A4;");
                text.setStyle("-fx-font-size: 13px; -fx-text-fill: #9AB2A4;");
            }
        }

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
            AnimationUtil.fadeSlideIn(newContent, 16, 0);
        } else {
            Node oldContent = currentContent;
            AnimationUtil.pageTransition(oldContent, newContent, () -> {
                contentArea.getChildren().setAll(newContent);
                currentContent = newContent;
            });
        }
    }

    private HBox buildOfflineBanner() {
        Label text = new Label("⚠  Backend Offline: Unable to connect to " + apiClient.getBaseUrl());
        text.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");

        Button retryBtn = new Button("↻ Retry Now");
        retryBtn.setStyle("-fx-background-color: white; -fx-text-fill: #BA4E4E; " +
                "-fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 4 12;");
        retryBtn.setOnAction(e -> checkOnline());

        HBox banner = new HBox(16, text, retryBtn);
        banner.setStyle("-fx-background-color: #BA4E4E; -fx-padding: 10 20;");
        banner.setAlignment(Pos.CENTER);
        return banner;
    }

    private void startHealthCheck() {
        Timeline tl = new Timeline(new KeyFrame(Duration.seconds(10), e -> checkOnline()));
        tl.setCycleCount(Timeline.INDEFINITE);
        tl.play();
        checkOnline();
    }

    private void checkOnline() {
        new Thread(() -> {
            boolean online = service.isBackendOnline();
            Platform.runLater(() -> {
                if (online && !isOnline) {
                    offlineBanner.setVisible(false);
                    offlineBanner.setManaged(false);
                    toast.showSuccess("Backend connected successfully!");
                    isOnline = true;
                    navigateTo(activeNavIndex); // reload active screen
                } else if (!online && isOnline) {
                    offlineBanner.setVisible(true);
                    offlineBanner.setManaged(true);
                    isOnline = false;
                }
            });
        }).start();
    }

    // ── Dedicated Settings Dialog ─────────────────────────────────
    private void showSettingsDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        dialog.setTitle("AttendX Preferences & System");

        VBox content = new VBox(20);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: #FAF8F5; -fx-border-color: #CCD6CF; -fx-border-radius: 14;");

        Label title = new Label("Settings & Configuration");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1B2B24;");

        // Motion setting
        CheckBox reduceMotionBox = new CheckBox("Reduce Motion & Disable Heavy Animations");
        reduceMotionBox.setSelected(AnimationUtil.isReduceMotion());
        reduceMotionBox.setStyle("-fx-text-fill: #1B2B24; -fx-font-size: 13px;");
        reduceMotionBox.selectedProperty().addListener((obs, old, val) -> {
            AnimationUtil.setReduceMotion(val);
            toast.showInfo(val ? "Motion reduced" : "Full animations enabled");
        });

        // API URL setting
        Label urlLbl = new Label("Backend REST API Base URL:");
        urlLbl.setStyle("-fx-text-fill: #5E7267; -fx-font-size: 12px; -fx-font-weight: bold;");

        TextField urlField = new TextField(apiClient.getBaseUrl());
        urlField.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #1B2B24; " +
                "-fx-border-color: #CCD6CF; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 12;");
        urlField.setPrefWidth(300);

        Label testResultLbl = new Label();
        testResultLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        Button testBtn = new Button("Test Connection");
        testBtn.setStyle("-fx-background-color: #EBF2EC; -fx-text-fill: #3E5E4C; " +
                "-fx-border-color: #4D735E; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 14; -fx-font-weight: bold;");
        testBtn.setOnAction(e -> {
            testBtn.setDisable(true);
            testResultLbl.setText("Testing...");
            testResultLbl.setStyle("-fx-text-fill: #5E7267;");
            new Thread(() -> {
                ApiClient testClient = new ApiClient(urlField.getText().trim());
                AttendXService testService = new AttendXService(testClient);
                boolean ok = testService.isBackendOnline();
                Platform.runLater(() -> {
                    testBtn.setDisable(false);
                    if (ok) {
                        testResultLbl.setText("✓ Connected! Server responded with 200 OK");
                        testResultLbl.setStyle("-fx-text-fill: #36825B; -fx-font-size: 12px; -fx-font-weight: bold;");
                    } else {
                        testResultLbl.setText("✗ Unreachable: Could not connect to API");
                        testResultLbl.setStyle("-fx-text-fill: #BA4E4E; -fx-font-size: 12px; -fx-font-weight: bold;");
                    }
                });
            }).start();
        });

        HBox urlRow = new HBox(10, urlField, testBtn);
        urlRow.setAlignment(Pos.CENTER_LEFT);

        VBox apiBox = new VBox(6, urlLbl, urlRow, testResultLbl);

        // System info
        VBox sysInfo = new VBox(4);
        sysInfo.setPadding(new Insets(12));
        sysInfo.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E3DED6; -fx-border-radius: 10; -fx-background-radius: 10;");
        Label i1 = new Label("• Java Runtime: " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        Label i2 = new Label("• Operating System: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        Label i3 = new Label("• Palette: Sage Green (#4D735E) & Cream White (#FAF8F5)");
        for (Label l : List.of(i1, i2, i3)) {
            l.setStyle("-fx-text-fill: #5E7267; -fx-font-size: 11px;");
        }
        sysInfo.getChildren().addAll(i1, i2, i3);

        Button closeBtn = new Button("Done");
        closeBtn.setStyle("-fx-background-color: #4D735E; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 8 24; -fx-font-weight: bold; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> {
            String newUrl = urlField.getText().trim();
            if (!newUrl.isEmpty() && !newUrl.equals(apiClient.getBaseUrl())) {
                System.setProperty("attendx.api", newUrl);
                apiClient = new ApiClient(newUrl);
                service = new AttendXService(apiClient);
                dashboardCtrl      = new DashboardController(service, toast);
                studentsCtrl       = new StudentsController(service, toast);
                markAttendanceCtrl = new MarkAttendanceController(service, toast);
                dailyReportCtrl    = new DailyReportController(service, toast);
                percentageCtrl     = new PercentageController(service, toast);
                checkOnline();
                navigateTo(activeNavIndex);
            }
            dialog.close();
        });

        HBox footer = new HBox(closeBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);

        content.getChildren().addAll(title, reduceMotionBox, apiBox, sysInfo, footer);

        Scene s = new Scene(content, 480, 420);
        dialog.setScene(s);
        dialog.showAndWait();
    }
}
