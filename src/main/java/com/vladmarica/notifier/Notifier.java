package com.vladmarica.notifier;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

import java.util.*;

public class Notifier {
    static String NOTIFICATIONS_BUS_NAME = "org.freedesktop.Notifications";
    static String NOTIFICATIONS_OBJECT_NAME = "/org/freedesktop/Notifications";
    private static NotificationClosedListener NO_OP_CLOSED_LISTENER = (id, reason) -> {};

    private static Map<Integer, Notification.ClosedReason> valueToClosedReasonEnumMap = new HashMap<>();
    static {
        for (Notification.ClosedReason reasonEnum : Notification.ClosedReason.values()) {
            valueToClosedReasonEnumMap.put(reasonEnum.value, reasonEnum);
        }
    }

    private DBusConnection connection;
    private DBusNotificationsInterface notificationsInterface;
    private Map<Long, LinkedList<NotificationClosedListener>> idToClosedListenerListMap = new HashMap<>();
    private Map<Long, Map<String, NotificationActionListener>> idToActionListenerMap = new HashMap<>();

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

        // Insert a no-op listener if none is given. This ensures listener are called in the correct order when a
        // notification is replaced multiple times
        NotificationClosedListener closedListener = notification.getClosedListener();
        if (closedListener == null) {
            closedListener = NO_OP_CLOSED_LISTENER;
        }

        // The freedesktop.org notification spec says that actions are defined as a two subsequent strings in an array.
        // The first string is a unique action ID, the second is the action text to display in the button.
        // Example array for two actions: {"view", "View Photo", "close", "Close"}
        List<Notification.Action> actionsList = notification.getActions();
        String[] actionsArray = new String[actionsList.size() * 2];
        Map<String, NotificationActionListener> actionListenerMap = new HashMap<>();
        for (int i = 0; i < actionsList.size(); i++) {
            Notification.Action action = actionsList.get(i);
            actionsArray[i * 2] = action.id;
            actionsArray[(i * 2) + 1] = action.displayText;
            actionListenerMap.put(action.id, action.actionListener);
        }

        Map<String, Variant> hints = new HashMap<>();
        if (notification.getImagePath() != null) {
            hints.put("image-path", new Variant<>(notification.getImagePath()));
        }

        UInt32 id = notificationsInterface.sendNotification(
                notification.getApplicationName(),
                new UInt32(0),
                notification.getIcon(),
                notification.getTitle(),
                notification.getBody(),
                actionsArray,
                hints,
                notification.getTimeout());

        synchronized (this) {
            addListenerToQueue(idToClosedListenerListMap, id.longValue(), closedListener);
            idToActionListenerMap.put(id.longValue(), actionListenerMap);
        }

        return id.longValue();
    }

    public void closeNotification(long notificationId) {
        notificationsInterface.closeNotification(new UInt32(notificationId));
    }

    public void close() {
        if (connection.isConnected()) {
            connection.disconnect();
        }
    }

    private synchronized void notificationClosedSignalHandler(DBusNotificationsInterface.NotificationClosed signal) {
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

    private synchronized void actionInvokedSignalHandler(DBusNotificationsInterface.ActionInvoked signal) {
        long id = signal.notificationId.longValue();

        if (idToActionListenerMap.containsKey(id)) {
            Map<String, NotificationActionListener> actionIdToListenerMap = idToActionListenerMap.get(id);
            actionIdToListenerMap.get(signal.action).onActionInvoked(id);
            idToActionListenerMap.remove(id);
        }
    }

    public static Notifier create() throws NotificationsException {
        try {
            return new Notifier(DBusConnection.getConnection(DBusConnection.DBusBusType.SESSION));
        }
        catch (DBusException ex) {
            throw new NotificationsException(ex);
        }
    }

    // VisibleForTesting
    static Notifier create(String dbusPath) throws NotificationsException{
        try {
            return new Notifier(DBusConnection.getConnection(dbusPath));
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
}
