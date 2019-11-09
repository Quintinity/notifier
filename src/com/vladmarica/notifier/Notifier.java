package com.vladmarica.notifier;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.UInt32;

import java.util.*;

public class Notifier {
    private static String NOTIFICATIONS_BUS_NAME = "org.freedesktop.Notifications";
    private static String NOTIFICATIONS_OBJECT_NAME = "/org/freedesktop/Notifications";
    private static NotificationClosedListener NO_OP_CLOSED_LISTENER = (id, reason) ->
            System.out.println("No-op listener called for notification " + id);

    private static Map<Integer, Notification.ClosedReason> valueToClosedReasonEnumMap = new HashMap<>();
    static {
        for (Notification.ClosedReason reasonEnum : Notification.ClosedReason.values()) {
            valueToClosedReasonEnumMap.put(reasonEnum.value, reasonEnum);
        }
    }

    private DBusConnection connection;
    private DBusNotificationsInterface notificationsInterface;
    private Map<Long, LinkedList<NotificationClosedListener>> idToClosedListenerListMap = new HashMap<>();

    private Notifier(DBusConnection connection) throws NotificationsException {
        try {
            this.connection = connection;

            this.notificationsInterface = connection.getRemoteObject(
                    NOTIFICATIONS_BUS_NAME,
                    NOTIFICATIONS_OBJECT_NAME,
                    DBusNotificationsInterface.class);

            this.connection.addSigHandler(DBusNotificationsInterface.NotificationClosed.class,
                    this.notificationsInterface,
                    this::notificationClosedSignalHandler);

            this.connection.addSigHandler(DBusNotificationsInterface.ActionInvoked.class,
                    this.notificationsInterface,
                    this::actionInvokedSignalHandler);
        }
        catch (DBusException ex) {
            throw new NotificationsException(ex);
        }
    }

    public String[] getCapabilities() {
        return notificationsInterface.getCapabilities();
    }

    public long send(Notification notification) {
        if (notification == null) {
            throw new IllegalStateException();
        }

        // Insert a no-op listener in the case that
        NotificationClosedListener closedListener = notification.getClosedListener();
        if (closedListener == null) {
            closedListener = NO_OP_CLOSED_LISTENER;
        }

        // The freedesktop.org notification spec says that actions are defined as a two subsequent strings in an array.
        // The first string is a unique action ID, the second is the action text to display in the button.
        // Example array for two actions: {"view", "View Photo", "close", "Close"}
        List<Notification.Action> actionsList = notification.getActions();
        String[] actionsArray = new String[actionsList.size() * 2];
        for (int i = 0; i < actionsList.size(); i++) {
            Notification.Action action = actionsList.get(i);
            actionsArray[i * 2] = action.id;
            actionsArray[(i * 2) + 1] = action.displayText;
        }

        UInt32 id = notificationsInterface.sendNotification(
                notification.getApplicationName(),
                new UInt32(0),
                "",
                notification.getTitle(),
                notification.getBody(),
                actionsArray,
                new HashMap<>(),
                -1);

        addListenerToQueue(idToClosedListenerListMap, id.longValue(), closedListener);

        return id.longValue();
    }

    private void notificationClosedSignalHandler(DBusNotificationsInterface.NotificationClosed signal) {
        long id = signal.notificationId.longValue();

        if (idToClosedListenerListMap.containsKey(id)) {
            LinkedList<NotificationClosedListener> listenerQueue = idToClosedListenerListMap.get(id);
            Notification.ClosedReason reason = valueToClosedReasonEnumMap.get(signal.reasonId.intValue());
            listenerQueue.pop().onNotificationClosed(id, reason);
            if (listenerQueue.isEmpty()) {
                idToClosedListenerListMap.remove(id);
            }
        }
    }

    private void actionInvokedSignalHandler(DBusNotificationsInterface.ActionInvoked signal) {
        // TODO
    }

    public static Notifier create() throws NotificationsException {
        try {
            return new Notifier(DBusConnection.getConnection(DBusConnection.DBusBusType.SESSION));
        }
        catch (DBusException ex) {
            throw new NotificationsException(ex);
        }
    }

    private static <T> void addListenerToQueue(Map<Long, LinkedList<T>> map, long key, T listener) {
        LinkedList<T> listenerQueue = map.getOrDefault(key, null);
        if (listenerQueue != null) {
            listenerQueue.push(listener);
        }
        else {
            listenerQueue = new LinkedList<>();
            listenerQueue.push(listener);
            map.put(key, listenerQueue);
        }
    }

    public static void main(String[] args) {
        try {
            Notifier notifier = Notifier.create();
            Notification notification = new Notification("java-notifications-demo", "Notification Title", "This is the notification body!");
            notification.addAction("view", "View");
            notification.addAction("close", "Close");
            notification.setClosedListener((id, reason) -> {
                System.out.println("Custom closed listener called for notification " + id);
            });
            long id = notifier.send(notification);
            System.out.println("Notification sent, id = " + id);
        }
        catch (NotificationsException ex) {
            ex.printStackTrace();
        }
    }
}
