package org.yagi.motel.pipeline;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import org.yagi.motel.bot.NotificationType;
import org.yagi.motel.bot.discord.utils.DiscordChannelUtils;
import org.yagi.motel.config.AppConfig;
import org.yagi.motel.model.container.CommunicationPlatformsContainer;
import org.yagi.motel.model.container.NotificationContainer;

@SuppressWarnings("checkstyle:MissingJavadocType")
public class NotificationHandler implements Runnable {

  private final BlockingQueue<NotificationContainer> notificationsQueue;
  private final CommunicationPlatformsContainer communicationPlatformsContainer;
  private final AppConfig config;

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public NotificationHandler(
      BlockingQueue<NotificationContainer> notificationsQueue,
      CommunicationPlatformsContainer communicationPlatformsContainer,
      AppConfig config) {
    this.notificationsQueue = notificationsQueue;
    this.communicationPlatformsContainer = communicationPlatformsContainer;
    this.config = config;
  }

  @Override
  public void run() {
    do {
      try {
        NotificationContainer notificationContainer = notificationsQueue.take();
        if (notificationContainer.getPlatformType() != null) {
          switch (notificationContainer.getPlatformType()) {
            case TG:
              Long tgNotificationChatId =
                  NotificationType.ADMIN == notificationContainer.getNotificationType()
                      ? config.getTelegram().getTgAdminChatId()
                      : config.getTelegram().getTournamentChatId();
              communicationPlatformsContainer
                  .getTgBot()
                  .sendNotification(notificationContainer.getMessage(), tgNotificationChatId);
              break;
            case DISCORD:
              Long discordNotificationChatId = config.getDiscord().getDiscordAdminChatId();

              Optional<Long> notificationChatId =
                  DiscordChannelUtils.getTargetChatIdFromNotificationContainer(
                      notificationContainer, config);
              if (notificationChatId.isPresent()) {
                discordNotificationChatId = notificationChatId.get();
              }

              communicationPlatformsContainer
                  .getDiscordBot()
                  .sendNotification(notificationContainer.getMessage(), discordNotificationChatId);
              break;
            default:
              break;
          }
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    } while (!Thread.interrupted());
  }
}
