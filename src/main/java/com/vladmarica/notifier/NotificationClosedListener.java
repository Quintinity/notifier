package com.vladmarica.notifier;

@FunctionalInterface
public interface NotificationClosedListener {
    void onNotificationClosed(long notificationId, Notification.ClosedReason reason);
}

