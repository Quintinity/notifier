package com.vladmarica.notifier;

import java.util.ArrayList;
import java.util.List;

public class Notification {

    public static final int TIMEOUT_DEFAULT = -1;
    public static final int TIMEOUT_NEVER_EXPIRE = 0;

    public enum ClosedReason {
        EXPIRED(1),
        DISMISSED(2),
        CLOSED(3),
        UNKNOWN(4);

        int value;
        ClosedReason(int value) {
            this.value = value;
        }
    }

    private String appName;
    private String title;
    private String body;
    private NotificationClosedListener closedListener;
    private List<Action> actions;
    private String icon = "";
    private int timeoutMs = TIMEOUT_DEFAULT;
    private String imagePath = null;

    public Notification(String appName, String title, String body) {
        this.appName = appName;
        this.title = title;
        this.body = body;
        this.actions = new ArrayList<>();
    }

    public void setClosedListener(NotificationClosedListener listener) {
        this.closedListener = listener;
    }

    public NotificationClosedListener getClosedListener() {
        return this.closedListener;
    }

    public String getApplicationName() {
        return this.appName;
    }

    public String getTitle() {
        return this.title;
    }

    public String getBody() {
        return this.body;
    }

    public void setIcon(String iconName) {
        this.icon = iconName;
    }

    public String getIcon() {
        return this.icon;
    }

    public void setTimeout(int ms) {
        if (ms < TIMEOUT_DEFAULT) {
            throw new IllegalArgumentException("Invalid timeout, must be positive or TIMEOUT_DEFAULT");
        }

        this.timeoutMs = ms;
    }

    public int getTimeout() {
        return this.timeoutMs;
    }

    public void setImagePath(String path) {
        this.imagePath = path;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void addAction(String actionId, String displayText, NotificationActionListener actionListener) {
        for (Action existingAction : this.actions) {
            if (existingAction.id.equals(actionId)) {
                throw new IllegalArgumentException(
                        String.format("An action with ID %s already exists on this notification", actionId));
            }
        }

        actions.add(new Action(actionId, displayText, actionListener));
    }

    public List<Action> getActions() {
        return this.actions;
    }

    public class Action {
        public final String id;
        public final String displayText;
        public NotificationActionListener actionListener;

        public Action(String id, String displayText, NotificationActionListener actionListener) {
            this.id = id;
            this.displayText = displayText;
            this.actionListener = actionListener;
        }
    }

}
