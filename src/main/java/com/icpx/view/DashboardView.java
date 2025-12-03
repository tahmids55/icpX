package com.icpx.view;

import com.icpx.database.UserDAO;
import com.icpx.database.SettingsDAO;
import com.icpx.model.User;
import com.icpx.util.SceneManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

/**
 * Main dashboard view with sidebar, topbar, and content area
 */
public class DashboardView {
    
    private static BorderPane mainRoot;
    private static ScrollPane contentScrollPane;

    public static Scene createScene() {
        mainRoot = new BorderPane();
        mainRoot.getStyleClass().add(SceneManager.getCurrentTheme() + "-theme");

        // Top Bar
        HBox topBar = createTopBar();
        mainRoot.setTop(topBar);

        // Sidebar
        VBox sidebar = createSidebar();
        mainRoot.setLeft(sidebar);

        // Content Area with ScrollPane
        contentScrollPane = new ScrollPane();
        contentScrollPane.setContent(createContentArea());
        contentScrollPane.setFitToWidth(true);
        contentScrollPane.getStyleClass().add("scroll-pane");
        contentScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        contentScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        mainRoot.setCenter(contentScrollPane);

        Scene scene = new Scene(mainRoot, 1000, 700);
        scene.getStylesheets().add(DashboardView.class.getResource("/styles.css").toExternalForm());
        
        return scene;
    }

    private static void switchContent(VBox newContent) {
        contentScrollPane.setContent(newContent);
    }

    private static HBox createTopBar() {
        HBox topBar = new HBox(20);
        topBar.getStyleClass().add("topbar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(16, 24, 16, 24));

        // Logo and app name container
        HBox logoContainer = new HBox(10);
        logoContainer.setAlignment(Pos.CENTER_LEFT);
        
        // Create logo using shapes
        StackPane logo = createLogo();
        
        // App name
        Text appName = new Text("icpX");
        appName.getStyleClass().add("app-logo");
        
        logoContainer.getChildren().addAll(logo, appName);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Theme toggle
        ToggleButton themeToggle = new ToggleButton("🌙 Dark");
        themeToggle.getStyleClass().add("toggle-button");
        themeToggle.setSelected(SceneManager.getCurrentTheme().equals("dark"));
        
        if (themeToggle.isSelected()) {
            themeToggle.setText("☀️ Light");
        }

        themeToggle.setOnAction(e -> {
            String newTheme = themeToggle.isSelected() ? "dark" : "light";
            SceneManager.setTheme(newTheme);
            themeToggle.setText(themeToggle.isSelected() ? "☀️ Light" : "🌙 Dark");
            
            // Refresh scene
            SceneManager.switchScene(DashboardView.createScene());
        });

        // Logout button
        Button logoutButton = new Button("Logout");
        logoutButton.getStyleClass().addAll("button", "button-danger");
        logoutButton.setOnAction(e -> {
            try {
                User user = UserDAO.getCurrentUser();
                if (user != null && user.isStartupPasswordEnabled()) {
                    SceneManager.switchScene(LoginView.createScene());
                } else {
                    // If startup password is not enabled, just close the app
                    SceneManager.getPrimaryStage().close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        topBar.getChildren().addAll(logoContainer, spacer, themeToggle, logoutButton);
        return topBar;
    }

    private static VBox createSidebar() {
        VBox sidebar = new VBox(15);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(250);
        sidebar.setPadding(new Insets(20));

        Text sidebarTitle = new Text("Navigation");
        sidebarTitle.getStyleClass().add("label");
        sidebarTitle.setStyle("-fx-font-size: 16px;");

        // Menu items
        Button dashboardBtn = new Button("📊 Dashboard");
        dashboardBtn.getStyleClass().addAll("button", "button-secondary");
        dashboardBtn.setPrefWidth(210);
        dashboardBtn.setOnAction(e -> switchContent(createContentArea()));

        Button targetsBtn = new Button("🎯 Target Section");
        targetsBtn.getStyleClass().addAll("button", "button-secondary");
        targetsBtn.setPrefWidth(210);
        targetsBtn.setOnAction(e -> switchContent(TargetView.createTargetView()));

        Button settingsBtn = new Button("⚙️ Settings");
        settingsBtn.getStyleClass().addAll("button", "button-secondary");
        settingsBtn.setPrefWidth(210);
        settingsBtn.setOnAction(e -> switchContent(createSettingsView()));

        sidebar.getChildren().addAll(
            sidebarTitle,
            new Separator(),
            dashboardBtn,
            targetsBtn,
            settingsBtn
        );

        return sidebar;
    }

    private static VBox createContentArea() {
        VBox contentArea = new VBox(20);
        contentArea.getStyleClass().add("content-area");
        contentArea.setPadding(new Insets(30));

        try {
            User user = UserDAO.getCurrentUser();
            
            // Welcome message
            Text welcomeText = new Text("Welcome, " + (user != null ? user.getUsername() : "User") + "!");
            welcomeText.getStyleClass().add("title");

            Text subtitle = new Text("Track your competitive programming journey");
            subtitle.getStyleClass().add("subtitle");

            // Statistics Overview
            HBox statsContainer = new HBox(20);
            statsContainer.setAlignment(Pos.CENTER_LEFT);
            
            // Total Solved Problems Card
            VBox solvedCard = createStatCard("Total Problems Solved", "0", "Solved", e -> {
                // TODO: Navigate to solved problems page
                System.out.println("Navigate to solved problems");
            });
            
            // Topics Learned Card
            VBox topicsCard = createStatCard("Topics Learned", "0", "Learned", e -> {
                // TODO: Navigate to learned topics page
                System.out.println("Navigate to learned topics");
            });
            
            statsContainer.getChildren().addAll(solvedCard, topicsCard);

            // Rating Graph Card
            VBox ratingGraphCard = new VBox(15);
            ratingGraphCard.getStyleClass().add("card");
            ratingGraphCard.setPadding(new Insets(20));
            ratingGraphCard.setMaxWidth(Double.MAX_VALUE);

            Text ratingTitle = new Text("Rating Graph");
            ratingTitle.getStyleClass().add("label");
            ratingTitle.setStyle("-fx-font-size: 18px;");

            // Placeholder for rating graph
            Pane ratingGraph = createRatingGraph();
            
            ratingGraphCard.getChildren().addAll(ratingTitle, new Separator(), ratingGraph);

            // Heatmap Card
            VBox heatmapCard = new VBox(15);
            heatmapCard.getStyleClass().add("card");
            heatmapCard.setPadding(new Insets(20));
            heatmapCard.setMaxWidth(Double.MAX_VALUE);

            Text heatmapTitle = new Text("Activity Heatmap");
            heatmapTitle.getStyleClass().add("label");
            heatmapTitle.setStyle("-fx-font-size: 18px;");

            // Placeholder for heatmap
            Pane heatmap = createHeatmap();
            
            heatmapCard.getChildren().addAll(heatmapTitle, new Separator(), heatmap);

            // Settings card
            VBox settingsCard = new VBox(15);
            settingsCard.getStyleClass().add("card");
            settingsCard.setMaxWidth(500);
            settingsCard.setPadding(new Insets(20));

            Text settingsTitle = new Text("Quick Settings");
            settingsTitle.getStyleClass().add("label");
            settingsTitle.setStyle("-fx-font-size: 18px;");

            Button goToSettingsBtn = new Button("Go to Settings");
            goToSettingsBtn.getStyleClass().addAll("button", "button-primary");
            goToSettingsBtn.setOnAction(e -> switchContent(createSettingsView()));
            goToSettingsBtn.setPrefWidth(200);

            Text settingsInfo = new Text("Configure Codeforces handle, startup password, and more");
            settingsInfo.getStyleClass().add("subtitle");

            settingsCard.getChildren().addAll(
                settingsTitle,
                new Separator(),
                goToSettingsBtn,
                settingsInfo
            );

            contentArea.getChildren().addAll(
                welcomeText,
                subtitle,
                new VBox(10),
                statsContainer,
                ratingGraphCard,
                heatmapCard,
                settingsCard
            );

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return contentArea;
    }
    
    private static VBox createStatCard(String title, String value, String buttonText, javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));
        card.setPrefWidth(240);
        card.setAlignment(Pos.CENTER);
        
        Text titleText = new Text(title);
        titleText.getStyleClass().add("subtitle");
        titleText.setWrappingWidth(220);
        titleText.setStyle("-fx-text-alignment: center;");
        
        Text valueText = new Text(value);
        valueText.getStyleClass().add("title");
        valueText.setStyle("-fx-font-size: 48px;");
        
        Button actionButton = new Button(buttonText);
        actionButton.getStyleClass().add("button");
        actionButton.setOnAction(action);
        actionButton.setPrefWidth(180);
        
        card.getChildren().addAll(titleText, valueText, actionButton);
        return card;
    }
    
    /**
     * Creates the rating graph visualization.
     * TODO: Integrate actual rating data from your rating mechanism.
     * Replace the sample data (xPoints, yPoints) with real user rating history.
     * You can modify this method to accept rating data as parameters.
     */
    private static Pane createRatingGraph() {
        VBox graphContainer = new VBox(10);
        graphContainer.setPrefHeight(300);
        graphContainer.setAlignment(Pos.CENTER);
        
        boolean isDark = SceneManager.getCurrentTheme().equals("dark");
        
        // Create canvas for the graph
        Canvas canvas = new Canvas(800, 250);
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // Draw axes
        gc.setStroke(isDark ? Color.web("#5a5a5a") : Color.web("#e2e8f0"));
        gc.setLineWidth(2);
        
        // Y-axis
        gc.strokeLine(50, 20, 50, 230);
        // X-axis
        gc.strokeLine(50, 230, 780, 230);
        
        // TODO: Replace this sample data with actual rating data from database
        // Draw sample rating line (placeholder data)
        gc.setStroke(isDark ? Color.web("#4fc3f7") : Color.web("#2563eb"));
        gc.setLineWidth(3);
        
        // Sample data - replace with real user rating history
        double[] xPoints = {50, 150, 250, 350, 450, 550, 650, 750};
        double[] yPoints = {200, 180, 160, 140, 120, 100, 90, 80};
        
        for (int i = 0; i < xPoints.length - 1; i++) {
            gc.strokeLine(xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);
            
            // Draw points
            gc.setFill(isDark ? Color.web("#4fc3f7") : Color.web("#2563eb"));
            gc.fillOval(xPoints[i] - 4, yPoints[i] - 4, 8, 8);
        }
        gc.fillOval(xPoints[xPoints.length - 1] - 4, yPoints[yPoints.length - 1] - 4, 8, 8);
        
        // Draw grid lines
        gc.setStroke(isDark ? Color.web("#3e3e42") : Color.web("#f1f5f9"));
        gc.setLineWidth(1);
        for (int i = 1; i < 6; i++) {
            double y = 30 + i * 40;
            gc.strokeLine(50, y, 780, y);
        }
        
        // Add labels
        gc.setFill(isDark ? Color.web("#9d9d9d") : Color.web("#64748b"));
        gc.fillText("Rating: 0 → 1500", 60, 250);
        
        Text noDataText = new Text("Start solving problems to see your rating progress!");
        noDataText.getStyleClass().add("subtitle");
        noDataText.setStyle("-fx-font-style: italic;");
        
        graphContainer.getChildren().addAll(canvas, noDataText);
        return graphContainer;
    }
    
    /**
     * Creates the activity heatmap visualization.
     * TODO: Integrate actual activity/solving data from your heatmap mechanism.
     * Replace the random activity values with real user activity data from database.
     * You can modify this method to accept activity data as parameters.
     */
    private static Pane createHeatmap() {
        VBox heatmapContainer = new VBox(10);
        heatmapContainer.setAlignment(Pos.CENTER_LEFT);
        
        boolean isDark = SceneManager.getCurrentTheme().equals("dark");
        
        // Create grid for heatmap (52 weeks x 7 days)
        GridPane grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);
        
        // Day labels
        String[] days = {"Mon", "Wed", "Fri"};
        int[] dayIndices = {1, 3, 5};
        
        for (int i = 0; i < days.length; i++) {
            Text dayLabel = new Text(days[i]);
            dayLabel.getStyleClass().add("subtitle");
            dayLabel.setStyle("-fx-font-size: 10px;");
            grid.add(dayLabel, 0, dayIndices[i]);
        }
        
        // Create heatmap cells (12 months view)
        for (int week = 0; week < 52; week++) {
            for (int day = 0; day < 7; day++) {
                Rectangle cell = new Rectangle(12, 12);
                
                // TODO: Replace random data with actual user activity from database
                // Sample activity data (random for demonstration)
                int activity = (int) (Math.random() * 5);
                
                String color;
                if (activity == 0) {
                    color = isDark ? "#1e1e1e" : "#f1f5f9";
                } else if (activity == 1) {
                    color = isDark ? "#0e4429" : "#9be9a8";
                } else if (activity == 2) {
                    color = isDark ? "#006d32" : "#40c463";
                } else if (activity == 3) {
                    color = isDark ? "#26a641" : "#30a14e";
                } else {
                    color = isDark ? "#39d353" : "#216e39";
                }
                
                cell.setFill(Color.web(color));
                cell.setStroke(isDark ? Color.web("#2d2d30") : Color.web("#e2e8f0"));
                
                grid.add(cell, week + 1, day);
            }
        }
        
        // Legend
        HBox legend = new HBox(8);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setPadding(new Insets(10, 0, 0, 0));
        
        Text lessText = new Text("Less");
        lessText.getStyleClass().add("subtitle");
        lessText.setStyle("-fx-font-size: 11px;");
        
        HBox legendBoxes = new HBox(4);
        String[] legendColors = isDark 
            ? new String[]{"#1e1e1e", "#0e4429", "#006d32", "#26a641", "#39d353"}
            : new String[]{"#f1f5f9", "#9be9a8", "#40c463", "#30a14e", "#216e39"};
            
        for (String color : legendColors) {
            Rectangle legendCell = new Rectangle(12, 12);
            legendCell.setFill(Color.web(color));
            legendCell.setStroke(isDark ? Color.web("#2d2d30") : Color.web("#e2e8f0"));
            legendBoxes.getChildren().add(legendCell);
        }
        
        Text moreText = new Text("More");
        moreText.getStyleClass().add("subtitle");
        moreText.setStyle("-fx-font-size: 11px;");
        
        legend.getChildren().addAll(lessText, legendBoxes, moreText);
        
        Text noDataText = new Text("No activity data yet. Start solving to fill the heatmap!");
        noDataText.getStyleClass().add("subtitle");
        noDataText.setStyle("-fx-font-style: italic;");
        
        heatmapContainer.getChildren().addAll(grid, legend, new VBox(5), noDataText);
        return heatmapContainer;
    }
    
    private static StackPane createLogo() {
        StackPane logoPane = new StackPane();
        logoPane.setPrefSize(32, 32);
        logoPane.setMaxSize(32, 32);
        logoPane.setMinSize(32, 32);
        
        // Determine current theme
        boolean isDark = SceneManager.getCurrentTheme().equals("dark");
        
        // Background circle
        Circle background = new Circle(16);
        background.setFill(isDark ? Color.web("#0e639c") : Color.web("#2563eb"));
        
        // Create "X" shape using two rectangles rotated
        Rectangle bar1 = new Rectangle(18, 3);
        bar1.setFill(Color.WHITE);
        bar1.setRotate(45);
        
        Rectangle bar2 = new Rectangle(18, 3);
        bar2.setFill(Color.WHITE);
        bar2.setRotate(-45);
        
        // Create connection point (small circle at center)
        Circle centerDot = new Circle(3);
        centerDot.setFill(isDark ? Color.web("#4fc3f7") : Color.web("#60a5fa"));
        
        logoPane.getChildren().addAll(background, bar1, bar2, centerDot);
        
        return logoPane;
    }

    private static VBox createSettingsView() {
        VBox settingsView = new VBox(20);
        settingsView.getStyleClass().add("content-area");
        settingsView.setPadding(new Insets(30));

        Text title = new Text("⚙️ Settings");
        title.getStyleClass().add("title");

        // Codeforces Handle Section
        VBox cfHandleCard = new VBox(15);
        cfHandleCard.getStyleClass().add("card");
        cfHandleCard.setPadding(new Insets(20));
        cfHandleCard.setMaxWidth(600);

        Text cfHandleTitle = new Text("Codeforces Handle");
        cfHandleTitle.getStyleClass().add("label");
        cfHandleTitle.setStyle("-fx-font-size: 18px;");

        HBox handleInputBox = new HBox(10);
        handleInputBox.setAlignment(Pos.CENTER_LEFT);

        TextField cfHandleField = new TextField();
        cfHandleField.setPromptText("Enter your Codeforces handle");
        cfHandleField.getStyleClass().add("text-field");
        cfHandleField.setPrefWidth(300);

        // Load saved handle
        String savedHandle = SettingsDAO.getCodeforcesHandle();
        if (savedHandle != null && !savedHandle.isEmpty()) {
            cfHandleField.setText(savedHandle);
        }

        Button saveHandleBtn = new Button("Save Handle");
        saveHandleBtn.getStyleClass().addAll("button", "button-primary");
        saveHandleBtn.setOnAction(e -> {
            String handle = cfHandleField.getText().trim();
            if (!handle.isEmpty()) {
                if (SettingsDAO.setCodeforcesHandle(handle)) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Success");
                    alert.setHeaderText(null);
                    alert.setContentText("Codeforces handle saved successfully!");
                    alert.showAndWait();
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText(null);
                    alert.setContentText("Failed to save Codeforces handle");
                    alert.showAndWait();
                }
            }
        });

        handleInputBox.getChildren().addAll(cfHandleField, saveHandleBtn);

        Text cfHandleInfo = new Text("Required to check problem status automatically in Target Section");
        cfHandleInfo.getStyleClass().add("subtitle");

        cfHandleCard.getChildren().addAll(cfHandleTitle, new Separator(), handleInputBox, cfHandleInfo);

        // Startup Password Section
        VBox passwordCard = new VBox(15);
        passwordCard.getStyleClass().add("card");
        passwordCard.setMaxWidth(600);
        passwordCard.setPadding(new Insets(20));

        Text passwordTitle = new Text("Startup Password");
        passwordTitle.getStyleClass().add("label");
        passwordTitle.setStyle("-fx-font-size: 18px;");

        CheckBox startupPasswordCheckbox = new CheckBox("Enable startup password");
        startupPasswordCheckbox.getStyleClass().add("check-box");
        
        try {
            User user = UserDAO.getCurrentUser();
            if (user != null) {
                startupPasswordCheckbox.setSelected(user.isStartupPasswordEnabled());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        startupPasswordCheckbox.setOnAction(e -> {
            try {
                boolean enabled = startupPasswordCheckbox.isSelected();
                UserDAO.toggleStartupPassword(enabled);
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Settings Updated");
                alert.setHeaderText(null);
                alert.setContentText("Startup password has been " + (enabled ? "enabled" : "disabled"));
                alert.showAndWait();
            } catch (Exception ex) {
                ex.printStackTrace();
                startupPasswordCheckbox.setSelected(!startupPasswordCheckbox.isSelected());
            }
        });

        Text passwordInfo = new Text("When enabled, you'll need to enter your password each time you open the app");
        passwordInfo.getStyleClass().add("subtitle");

        passwordCard.getChildren().addAll(passwordTitle, new Separator(), startupPasswordCheckbox, passwordInfo);

        settingsView.getChildren().addAll(title, cfHandleCard, passwordCard);

        return settingsView;
    }
}
