package com.icpx.view;

import com.icpx.database.UserDAO;
import com.icpx.model.User;
import com.icpx.util.SceneManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
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
    
    public static Scene createScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add(SceneManager.getCurrentTheme() + "-theme");

        // Top Bar
        HBox topBar = createTopBar();
        root.setTop(topBar);

        // Sidebar
        VBox sidebar = createSidebar();
        root.setLeft(sidebar);

        // Content Area
        VBox contentArea = createContentArea();
        root.setCenter(contentArea);

        Scene scene = new Scene(root, 1000, 700);
        scene.getStylesheets().add(DashboardView.class.getResource("/styles.css").toExternalForm());
        
        return scene;
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

        // Placeholder menu items
        Button dashboardBtn = new Button("📊 Dashboard");
        dashboardBtn.getStyleClass().addAll("button", "button-secondary");
        dashboardBtn.setPrefWidth(210);

        Button settingsBtn = new Button("⚙️ Settings");
        settingsBtn.getStyleClass().addAll("button", "button-secondary");
        settingsBtn.setPrefWidth(210);

        sidebar.getChildren().addAll(
            sidebarTitle,
            new Separator(),
            dashboardBtn,
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

            Text subtitle = new Text("Your dashboard is ready");
            subtitle.getStyleClass().add("subtitle");

            // Settings card
            VBox settingsCard = new VBox(15);
            settingsCard.getStyleClass().add("card");
            settingsCard.setMaxWidth(500);
            settingsCard.setPadding(new Insets(20));

            Text settingsTitle = new Text("Settings");
            settingsTitle.getStyleClass().add("label");
            settingsTitle.setStyle("-fx-font-size: 18px;");

            // Startup password toggle
            CheckBox startupPasswordCheckbox = new CheckBox("Enable startup password");
            startupPasswordCheckbox.getStyleClass().add("check-box");
            if (user != null) {
                startupPasswordCheckbox.setSelected(user.isStartupPasswordEnabled());
            }

            startupPasswordCheckbox.setOnAction(e -> {
                try {
                    boolean enabled = startupPasswordCheckbox.isSelected();
                    UserDAO.toggleStartupPassword(enabled);
                    
                    // Show confirmation
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

            Text startupPasswordInfo = new Text("When enabled, you'll need to enter your password each time you open the app");
            startupPasswordInfo.getStyleClass().add("subtitle");
            startupPasswordInfo.setWrappingWidth(450);

            settingsCard.getChildren().addAll(
                settingsTitle,
                new Separator(),
                startupPasswordCheckbox,
                startupPasswordInfo
            );

            contentArea.getChildren().addAll(
                welcomeText,
                subtitle,
                new VBox(10),
                settingsCard
            );

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return contentArea;
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
}
