package com.icpx.service;

import java.awt.*;
import java.awt.TrayIcon.MessageType;

public class NotificationService {

    private static TrayIcon trayIcon;

    public static void initialize() {
        try {
            if (GraphicsEnvironment.isHeadless()) {
                System.err.println("Running in headless environment; SystemTray not available.");
                return;
            }
            if (!SystemTray.isSupported()) {
                System.err.println("SystemTray is not supported");
                return;
            }

            SystemTray tray = SystemTray.getSystemTray();
            
            // Use a default icon or load one
            // For now, we'll create a simple 16x16 image if none exists
            java.awt.Image image = Toolkit.getDefaultToolkit().createImage(
                NotificationService.class.getResource("/icon.png")
            );
            
            // Fallback for demo if icon not found
            if (image == null) {
                 image = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            }

            trayIcon = new TrayIcon(image, "icpX");
            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip("icpX Competitive Programming");
            
            tray.add(trayIcon);
        } catch (Throwable t) {
            System.err.println("NotificationService could not be initialized: " + t.getMessage());
        }
    }

    public static void showNotification(String title, String message) {
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, MessageType.INFO);
        } else {
            // Log to console if tray not available
            System.out.println("NOTIFICATION: [" + title + "] " + message);
        }
    }
}
