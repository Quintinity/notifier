package com.vladmarica.notifier;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(JUnit4.class)
public class NotifierTest extends TestCase {

    private static final String DBUS_SOCKET_PATH = "unix:path=/run/user/1000/bus";

    @Test
    public void testSendNotification() throws Exception {
        Notifier notifier = Notifier.create(DBUS_SOCKET_PATH);

        Notification notification = new Notification("notifier-test", "Title", "Body");
        notification.setTimeout(500);

        AtomicBoolean called = new AtomicBoolean(false);
        AtomicReference<Notification.ClosedReason> closedReason = new AtomicReference<>(null);
        notification.setClosedListener((id, reason) -> {
            called.set(true);
            closedReason.set(reason);
        });

        notifier.send(notification);

        Thread.sleep(2000);
        assertTrue(called.get());
        assertEquals(Notification.ClosedReason.EXPIRED, closedReason.get());
        notifier.close();
    }

    @Test
    public void testSendAndCloseNotification() throws Exception {
        Notifier notifier = Notifier.create(DBUS_SOCKET_PATH);

        Notification notification = new Notification("notifier-test", "Title", "Body");
        notification.setTimeout(5000);

        AtomicReference<Notification.ClosedReason> closedReason = new AtomicReference<>(null);
        notification.setClosedListener((id, reason) -> {
            closedReason.set(reason);
        });

        long id = notifier.send(notification);
        Thread.sleep(1000);
        notifier.closeNotification(id);

        Thread.sleep(1000);
        assertEquals(Notification.ClosedReason.CLOSED, closedReason.get());
        notifier.close();
    }
}
