package com.vladmarica.notifier;


@FunctionalInterface
public interface NotificationActionListener {
    void onActionInvoked(long notificationId);
}
