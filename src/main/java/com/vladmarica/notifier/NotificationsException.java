package com.vladmarica.notifier;

public class NotificationsException extends Exception {
    public NotificationsException(Throwable throwable) {
        super(throwable);
    }

    public NotificationsException(String message) {
        super(message);
    }

    public NotificationsException() {
        super();
    }
}
