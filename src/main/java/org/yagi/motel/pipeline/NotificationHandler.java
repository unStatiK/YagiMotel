package org.yagi.motel.pipeline;

import org.yagi.motel.bot.NotificationType;
import org.yagi.motel.bot.TournamentHelper;
import org.yagi.motel.config.AppConfig;
import org.yagi.motel.model.container.NotificationContainer;

import java.util.concurrent.BlockingQueue;

public class NotificationHandler implements Runnable {

    private final BlockingQueue<NotificationContainer> notificationsQueue;
    private final TournamentHelper bot;
    private final AppConfig config;

    public NotificationHandler(BlockingQueue<NotificationContainer> notificationsQueue,
                               TournamentHelper bot, AppConfig config) {
        this.notificationsQueue = notificationsQueue;
        this.bot = bot;
        this.config = config;
    }

    @Override
    public void run() {
        do {
            try {
                NotificationContainer notificationContainer = notificationsQueue.take();
                Long notificationChatId = NotificationType.ADMIN == notificationContainer.getNotificationType() ?
                        config.getAdminChatId() : config.getTournamentChatId();
                bot.sendNotification(notificationContainer.getMessage(), notificationChatId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } while (!Thread.interrupted());
    }
}
