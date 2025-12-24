package com.cloudstorage.fx.components;

import com.cloudstorage.config.SessionManager;
import com.cloudstorage.database.NotificationDAO;
import com.cloudstorage.model.Notification;
import com.cloudstorage.model.User;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class NotificationPanelController {

    @FXML private VBox notificationsContainer;
    @FXML private VBox emptyState;
    @FXML private VBox loadingState;
    @FXML private javafx.scene.control.ScrollPane notificationsScrollPane;
    @FXML private javafx.scene.control.ProgressIndicator loadingSpinner;
    @FXML private Label unreadBadge;
    @FXML private Button markAllReadBtn;
    @FXML private Button clearAllBtn;
    @FXML private Button closeBtn;

    private Consumer<Void> onClose;
    private Runnable onNotificationRead;
    private Timeline autoRefreshTimeline;

    // Cache for optimization
    private List<Notification> cachedNotifications = new ArrayList<>();
    private int cachedUnreadCount = 0;
    private boolean isFirstLoad = true;

    @FXML
    public void initialize() {
        loadNotifications();
        startAutoRefresh();
    }

    /**
     * Starts auto-refresh timer to update notifications every 10 seconds
     */
    private void startAutoRefresh() {
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(10), event -> {
            loadNotifications();
        }));
        autoRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        autoRefreshTimeline.play();
    }

    /**
     * Stops the auto-refresh timer
     */
    public void stopAutoRefresh() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
    }

    /**
     * Sets callback when panel is closed
     */
    public void setOnClose(Consumer<Void> callback) {
        this.onClose = callback;
    }

    /**
     * Sets callback when notification is read (to update badge)
     */
    public void setOnNotificationRead(Runnable callback) {
        this.onNotificationRead = callback;
    }

    /**
     * Loads notifications from database (with caching to avoid unnecessary reloads)
     */
    public void loadNotifications() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        // If we have cached data, display it immediately
        if (!isFirstLoad && !cachedNotifications.isEmpty()) {
            displayCachedNotifications();
        } else if (isFirstLoad) {
            // Only show loading state on very first load (no cache available)
            showLoadingState();
        }

        // Fetch fresh data in background
        new Thread(() -> {
            try {
                List<Notification> notifications = NotificationDAO.getNotificationsByUserId(user.getId());
                int unreadCount = NotificationDAO.getUnreadCount(user.getId());

                // Check if there are any changes
                boolean hasChanges = isFirstLoad ||
                                    unreadCount != cachedUnreadCount ||
                                    !areNotificationsEqual(notifications, cachedNotifications);

                if (!hasChanges) {
                    // No changes detected, skip UI update
                    return;
                }

                // Update cache
                cachedNotifications = new ArrayList<>(notifications);
                cachedUnreadCount = unreadCount;
                isFirstLoad = false;

                Platform.runLater(() -> {
                    hideLoadingState();
                    updateNotificationUI(notifications, unreadCount);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(this::hideLoadingState);
            }
        }).start();
    }

    /**
     * Displays cached notifications immediately (no loading)
     */
    private void displayCachedNotifications() {
        Platform.runLater(() -> {
            updateNotificationUI(cachedNotifications, cachedUnreadCount);
        });
    }

    /**
     * Updates the notification UI with given data
     */
    private void updateNotificationUI(List<Notification> notifications, int unreadCount) {
        notificationsContainer.getChildren().clear();

        if (notifications.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
            for (Notification notification : notifications) {
                VBox card = createNotificationCard(notification);
                notificationsContainer.getChildren().add(card);
            }
        }

        // Update unread badge
        if (unreadCount > 0) {
            unreadBadge.setText(String.valueOf(unreadCount));
            unreadBadge.setVisible(true);
        } else {
            unreadBadge.setVisible(false);
        }
    }

    /**
     * Checks if two notification lists are equal (by comparing IDs and read status)
     */
    private boolean areNotificationsEqual(List<Notification> list1, List<Notification> list2) {
        if (list1.size() != list2.size()) {
            return false;
        }

        for (int i = 0; i < list1.size(); i++) {
            Notification n1 = list1.get(i);
            Notification n2 = list2.get(i);

            if (!n1.getId().equals(n2.getId()) || n1.isRead() != n2.isRead()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Shows loading state
     */
    private void showLoadingState() {
        Platform.runLater(() -> {
            notificationsScrollPane.setVisible(false);
            notificationsScrollPane.setManaged(false);
            emptyState.setVisible(false);
            emptyState.setManaged(false);
            loadingState.setVisible(true);
            loadingState.setManaged(true);
        });
    }

    /**
     * Hides loading state
     */
    private void hideLoadingState() {
        Platform.runLater(() -> {
            loadingState.setVisible(false);
            loadingState.setManaged(false);
            notificationsScrollPane.setVisible(true);
            notificationsScrollPane.setManaged(true);
        });
    }

    /**
     * Creates a notification card
     */
    private VBox createNotificationCard(Notification notification) {
        VBox card = new VBox(12);
        card.getStyleClass().add("notification-card");
        if (!notification.isRead()) {
            card.getStyleClass().add("unread");
        }
        card.setPadding(new Insets(16, 18, 16, 18));

        // Header: Icon + Title + Time + Actions
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        // Icon based on type
        FontIcon icon = new FontIcon(getIconForType(notification.getType()));
        icon.setIconSize(22);
        icon.setIconColor(Color.web(getColorForType(notification.getType())));
        StackPane iconContainer = new StackPane(icon);
        iconContainer.getStyleClass().add("notification-icon");
        iconContainer.setStyle(
            "-fx-background-color: " + getColorForType(notification.getType()) + "18; " +
            "-fx-background-radius: 12; " +
            "-fx-min-width: 44; " +
            "-fx-min-height: 44; " +
            "-fx-max-width: 44; " +
            "-fx-max-height: 44;"
        );

        // Title
        Label title = new Label(notification.getTitle());
        title.getStyleClass().add("notification-card-title");
        HBox.setHgrow(title, Priority.ALWAYS);

        // Time
        Label time = new Label(formatTime(notification.getCreatedAt()));
        time.getStyleClass().add("notification-time");

        // Delete button
        Button deleteBtn = new Button();
        deleteBtn.getStyleClass().add("notification-delete-btn");
        FontIcon deleteIcon = new FontIcon(FontAwesomeSolid.TIMES);
        deleteIcon.setIconSize(12);
        deleteIcon.setIconColor(Color.web("#94a3b8"));
        deleteBtn.setGraphic(deleteIcon);
        deleteBtn.setOnAction(e -> handleDeleteNotification(notification));

        header.getChildren().addAll(iconContainer, title, time, deleteBtn);

        // Message
        Label message = new Label(notification.getMessage());
        message.getStyleClass().add("notification-message");
        message.setWrapText(true);

        card.getChildren().addAll(header, message);

        // Action button if URL exists
        if (notification.getActionUrl() != null && !notification.getActionUrl().isEmpty()) {
            Button actionBtn = new Button("View");
            actionBtn.getStyleClass().add("notification-action-btn");
            actionBtn.setOnAction(e -> handleNotificationAction(notification));
            card.getChildren().add(actionBtn);
        }

        // Mark as read on click
        card.setOnMouseClicked(e -> {
            if (!notification.isRead()) {
                markNotificationAsRead(notification);
            }
        });

        return card;
    }

    /**
     * Handles notification action
     */
    private void handleNotificationAction(Notification notification) {
        // Mark as read
        if (!notification.isRead()) {
            markNotificationAsRead(notification);
        }

        // Handle action URL (could navigate to specific view)
        System.out.println("Action clicked: " + notification.getActionUrl());
        // TODO: Implement navigation based on actionUrl
    }

    /**
     * Forces a refresh by invalidating the cache
     */
    public void forceRefresh() {
        isFirstLoad = true;
        cachedNotifications.clear();
        cachedUnreadCount = -1;
        loadNotifications();
    }

    /**
     * Marks notification as read
     */
    private void markNotificationAsRead(Notification notification) {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        new Thread(() -> {
            boolean success = NotificationDAO.markAsRead(notification.getId(), user.getId());
            if (success) {
                Platform.runLater(() -> {
                    forceRefresh(); // Force refresh after marking as read
                    if (onNotificationRead != null) {
                        onNotificationRead.run();
                    }
                });
            }
        }).start();
    }

    /**
     * Handles delete notification
     */
    private void handleDeleteNotification(Notification notification) {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        new Thread(() -> {
            boolean success = NotificationDAO.deleteNotification(notification.getId(), user.getId());
            if (success) {
                Platform.runLater(() -> {
                    forceRefresh(); // Force refresh after deletion
                    if (onNotificationRead != null) {
                        onNotificationRead.run();
                    }
                });
            }
        }).start();
    }

    /**
     * Marks all notifications as read
     */
    @FXML
    private void handleMarkAllRead() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        new Thread(() -> {
            boolean success = NotificationDAO.markAllAsRead(user.getId());
            if (success) {
                Platform.runLater(() -> {
                    forceRefresh(); // Force refresh after marking all as read
                    if (onNotificationRead != null) {
                        onNotificationRead.run();
                    }
                });
            }
        }).start();
    }

    /**
     * Clears all read notifications
     */
    @FXML
    private void handleClearAll() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        new Thread(() -> {
            boolean success = NotificationDAO.deleteAllRead(user.getId());
            if (success) {
                Platform.runLater(this::forceRefresh); // Force refresh after clearing
            }
        }).start();
    }

    /**
     * Closes the notification panel
     */
    @FXML
    private void handleClose() {
        stopAutoRefresh();
        if (onClose != null) {
            onClose.accept(null);
        }
    }

    /**
     * Shows empty state
     */
    private void showEmptyState() {
        notificationsScrollPane.setVisible(false);
        notificationsScrollPane.setManaged(false);
        loadingState.setVisible(false);
        loadingState.setManaged(false);
        emptyState.setVisible(true);
        emptyState.setManaged(true);
    }

    /**
     * Hides empty state
     */
    private void hideEmptyState() {
        notificationsScrollPane.setVisible(true);
        notificationsScrollPane.setManaged(true);
        loadingState.setVisible(false);
        loadingState.setManaged(false);
        emptyState.setVisible(false);
        emptyState.setManaged(false);
    }

    /**
     * Gets icon for notification type
     */
    private FontAwesomeSolid getIconForType(String type) {
        switch (type.toLowerCase()) {
            case "success":
                return FontAwesomeSolid.CHECK_CIRCLE;
            case "warning":
                return FontAwesomeSolid.EXCLAMATION_TRIANGLE;
            case "danger":
            case "error":
                return FontAwesomeSolid.EXCLAMATION_CIRCLE;
            case "info":
            default:
                return FontAwesomeSolid.INFO_CIRCLE;
        }
    }

    /**
     * Gets color for notification type
     */
    private String getColorForType(String type) {
        switch (type.toLowerCase()) {
            case "success":
                return "#10B981"; // Green
            case "warning":
                return "#F59E0B"; // Orange
            case "danger":
            case "error":
                return "#EF4444"; // Red
            case "info":
            default:
                return "#3B82F6"; // Blue
        }
    }

    /**
     * Formats time relative to now
     */
    private String formatTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "";

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        long minutes = java.time.Duration.between(dateTime, now).toMinutes();

        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + "m ago";
        if (minutes < 1440) return (minutes / 60) + "h ago";
        if (minutes < 10080) return (minutes / 1440) + "d ago";

        return dateTime.format(DateTimeFormatter.ofPattern("MMM dd"));
    }
}

