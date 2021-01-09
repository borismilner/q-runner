package qa;

import osf.modules.notifications.Notification;
import osf.modules.notifications.NotificationManager;
import osf.modules.notifications.Notifier;
import osf.shared.annotations.InjectService;
import qa.structures.QModule;
import qa.structures.QResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@InjectService
public class QANotification extends QModule implements Consumer<List<Notification>> {

    private final NotificationManager notificationManager;


    private final Notifier notifier;
    private final Map<String, Notification> mapNameToNotification = new HashMap<>();

    private final List<Notification> receivedNotifications = new ArrayList<>();


    public QANotification(String serviceId, NotificationManager notificationManager) {
        super(serviceId, "Notification-QA-Module");
        this.notificationManager = notificationManager;
        this.notifier = notificationManager.getNotifier(this);
    }

    @Override
    public void accept(List<Notification> notifications) {
        this.receivedNotifications.addAll(notifications);
    }

    public NotificationManager getNotificationManager() {
        return this.notificationManager;
    }

    public Map<String, Object> createNotification(String notificationName, String topic, String data) {
        Map<String, Object> response;
        Notification newNotification = new Notification(topic, data);
        this.mapNameToNotification.put(notificationName, newNotification);
        return Map.of("notification created", newNotification);
    }

    public QResponse publish(String notificationName) {
        this.notificationManager.publish(this, this.mapNameToNotification.get(notificationName));
        return new QResponse(
                this.moduleId,
                Map.of("published", true)
        );
    }

    public QResponse subscribeToTopic(String topic) {
        this.notifier.subscribeToTopic(topic);
        return new QResponse(
                this.moduleId,
                Map.of("subscribed to", topic)
        );
    }

    public QResponse unsubscribeFromTopic(String topic) {
        this.notifier.unsubscribeFromTopic(topic);
        return new QResponse(
                this.moduleId,
                Map.of("unsubscribed from", topic)
        );
    }

    public QResponse pullNotifications() {
        List<Notification> notifications = this.notifier.pullNotifications();
        return new QResponse(
                this.moduleId,
                Map.of("notifications", notifications)
        );
    }

    public QResponse pullNotificationsFiltered(String topic, double sinceInSec) {
        long time = System.currentTimeMillis() - ((int) sinceInSec * 1000L);
        Instant instant = Instant.ofEpochSecond(time);
        List<Notification> notifications = this.notifier.pullNotificationsFiltered(topic, instant);
        return new QResponse(
                this.moduleId,
                Map.of("notifications", notifications)
        );
    }

    public QResponse startScheduledPushing(double timeToWait) {
        this.notifier.startScheduledPushing(this, (int) timeToWait, TimeUnit.SECONDS);
        return new QResponse(
                this.moduleId,
                Map.of("Scheduled pull every", String.format("%d seconds", timeToWait))
        );
    }


    public QResponse startRealtimePushing() {
        this.notifier.startRealtimePushing(this);
        return new QResponse(
                this.moduleId,
                Map.of("started real-time pushing", true)
        );
    }


    public QResponse stopPushing() {
        this.notifier.startRealtimePushing(this);
        return new QResponse(
                this.moduleId,
                Map.of("stopped real-time pushing", true)
        );
    }


    public QResponse clear() {
        this.notifier.clear();
        return new QResponse(
                this.moduleId,
                Map.of("cleared notifications", true)
        );
    }

    public QResponse getReceivedNotifications() {
        return new QResponse(
                this.moduleId,
                Map.of("notifications", this.receivedNotifications)
        );
    }


}
