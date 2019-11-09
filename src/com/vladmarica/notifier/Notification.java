package com.vladmarica.notifier;

import java.util.ArrayList;
import java.util.List;

public class Notification {

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

    public void addAction(String actionId, String displayText) {
        for (Action existingAction : this.actions) {
            if (existingAction.id.equals(actionId)) {
                throw new IllegalArgumentException(
                        String.format("An action with ID %s already exists on this notification", actionId));
            }
        }

        actions.add(new Action(actionId, displayText));
    }

    public List<Action> getActions() {
        return this.actions;
    }

    public class Action {
        public final String id;
        public final String displayText;

        public Action(String id, String displayText) {
            this.id = id;
            this.displayText = displayText;
        }
    }

}
