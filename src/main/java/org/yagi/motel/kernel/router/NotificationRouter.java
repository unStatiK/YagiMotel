package org.yagi.motel.kernel.router;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import org.yagi.motel.bot.discord.utils.DiscordChannelUtils;
import org.yagi.motel.config.AppConfig;
import org.yagi.motel.kernel.enums.NotificationType;
import org.yagi.motel.kernel.model.container.NotificationContainer;
import org.yagi.motel.kernel.model.container.ResultCommandContainer;
import org.yagi.motel.utils.PlatformUtils;

@SuppressWarnings("checkstyle:MissingJavadocType")
public class NotificationRouter implements Runnable {

  private final BlockingQueue<NotificationContainer> notificationsQueue;
  private final BlockingQueue<ResultCommandContainer> tgMessagesQueue;
  private final BlockingQueue<ResultCommandContainer> discordMessagesQueue;
  private final AppConfig config;

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public NotificationRouter(
      BlockingQueue<NotificationContainer> notificationsQueue,
      BlockingQueue<ResultCommandContainer> tgMessagesQueue,
      BlockingQueue<ResultCommandContainer> discordMessagesQueue,
      AppConfig config) {
    this.notificationsQueue = notificationsQueue;
    this.tgMessagesQueue = tgMessagesQueue;
    this.discordMessagesQueue = discordMessagesQueue;
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
              if (PlatformUtils.isPlatformEnable(config, notificationContainer.getPlatformType())) {
                Long tgNotificationChatId =
                    NotificationType.ADMIN == notificationContainer.getNotificationType()
                        ? config.getTelegram().getTgAdminChatId()
                        : config.getTelegram().getTournamentChatId();
                tgMessagesQueue.put(
                    ResultCommandContainer.builder()
                        .resultMessage(notificationContainer.getMessage())
                        .platformType(notificationContainer.getPlatformType())
                        .replyChatId(tgNotificationChatId)
                        .build());
              }
              break;
            case DISCORD:
              if (PlatformUtils.isPlatformEnable(config, notificationContainer.getPlatformType())) {
                Long discordNotificationChatId = config.getDiscord().getDiscordAdminChatId();

                Optional<Long> notificationChatId =
                    DiscordChannelUtils.getTargetChatIdFromNotificationContainer(
                        notificationContainer, config);
                if (notificationChatId.isPresent()) {
                  discordNotificationChatId = notificationChatId.get();
                }
                discordMessagesQueue.put(
                    ResultCommandContainer.builder()
                        .resultMessage(notificationContainer.getMessage())
                        .platformType(notificationContainer.getPlatformType())
                        .replyChatId(discordNotificationChatId)
                        .build());
              }
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
