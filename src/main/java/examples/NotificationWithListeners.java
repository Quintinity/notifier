package examples;

import com.vladmarica.notifier.*;

public class NotificationWithListeners {
    public static void main(String[] args) {
        try {
            final Notifier notifier = Notifier.create();

            Notification notification = new Notification(
                    "java-notifications-demo",
                    "Floppy Disk Inserted",
                    "A floppy disk has just been inserted into the machine.");
            notification.setIcon("media-floppy");
            notification.setTimeout(Notification.TIMEOUT_DEFAULT);

            notification.addAction("view", "View",
                    (notificationId) -> System.out.println("View clicked!"));
            notification.addAction("eject", "Eject",
                    (notificationId) -> System.out.println("Eject clicked!"));
            notification.setClosedListener(
                    (id, reason) -> System.out.println(String.format("Notification %d closed with reason %s", id, reason.toString())));

            long id = notifier.send(notification);
            System.out.println("Notification sent, id = " + id);

            Thread.sleep(5000);
            notifier.close(); // close the underlying DBus connection, stopping the threads and allowing the process to exit
        }
        catch (NotificationsException | InterruptedException ex) {
            ex.printStackTrace();
        }
    }
}
