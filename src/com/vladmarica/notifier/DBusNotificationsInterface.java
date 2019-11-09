package com.vladmarica.notifier;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusMemberName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

import java.util.Map;

@DBusInterfaceName("org.freedesktop.Notifications")
interface DBusNotificationsInterface extends DBusInterface {

    @DBusMemberName("GetCapabilities")
    String[] getCapabilities();

    @DBusMemberName("Notify")
    UInt32 sendNotification(String appName, UInt32 replaceId, String appIcon, String summary, String body,
                            String[] actions, Map<String, Variant> hints, int timeout);

    class NotificationClosed extends DBusSignal {
        public final UInt32 notificationId;
        public final UInt32 reasonId;

        public NotificationClosed(String path, UInt32 notificationId, UInt32 reasonId) throws DBusException {
            super(path, notificationId, reasonId);
            this.notificationId = notificationId;
            this.reasonId = reasonId;
        }
    }

    class ActionInvoked extends DBusSignal {
        public final UInt32 notificationId;
        public final String action;

        public ActionInvoked(String path, UInt32 notificationId, String action) throws DBusException {
            super(path, notificationId, action);
            this.notificationId = notificationId;
            this.action = action;
        }
    }
}
