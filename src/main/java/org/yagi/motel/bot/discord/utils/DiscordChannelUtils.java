package org.yagi.motel.bot.discord.utils;

import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.yagi.motel.bot.PortalNotificationLang;
import org.yagi.motel.bot.PortalNotificationMessageType;
import org.yagi.motel.config.AppConfig;
import org.yagi.motel.config.properties.DiscordProperties;
import org.yagi.motel.model.container.NotificationContainer;
import org.yagi.motel.model.enums.Lang;

@UtilityClass
@SuppressWarnings("checkstyle:MissingJavadocType")
public class DiscordChannelUtils {
  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static Optional<Long> getTargetChatIdFromNotificationContainer(
      NotificationContainer notificationContainer, AppConfig config) {
    Optional<Long> defaultNotificationChatId =
        getDiscordNotificationChatId(notificationContainer, config);
    String currentMessageType = notificationContainer.getMessageType();
    if (PortalNotificationMessageType.CONFIRMATION_STARTED
        .getMessageType()
        .equals(currentMessageType)) {
      return getDiscordNotificationChatId(notificationContainer, config);
    }

    return defaultNotificationChatId;
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static String getRequiredLangFromChannel(Long channelId, AppConfig config) {
    final DiscordProperties props = config.getDiscord();
    if (props.getRuNotificationTournamentChannelId().equals(channelId)) {
      return Lang.RU.getLang();
    }
    if (props.getEnNotificationTournamentChannelId().equals(channelId)) {
      return Lang.EN.getLang();
    }
    if (props.getRuConfirmationTournamentChannelId().equals(channelId)) {
      return Lang.RU.getLang();
    }
    if (props.getEnConfirmationTournamentChannelId().equals(channelId)) {
      return Lang.EN.getLang();
    }
    if (props.getGameLogsChannelId().equals(channelId)) {
      return Lang.EN.getLang();
    }
    return Lang.RU.getLang();
  }

  private static Optional<Long> getDiscordNotificationChatId(
      NotificationContainer notificationContainer, AppConfig config) {
    if (notificationContainer.getLang() == PortalNotificationLang.EN) {
      return Optional.of(config.getDiscord().getEnNotificationTournamentChannelId());
    }
    if (notificationContainer.getLang() == PortalNotificationLang.RU) {
      return Optional.of(config.getDiscord().getRuNotificationTournamentChannelId());
    }
    return Optional.empty();
  }

  private static Optional<Long> getDiscordConfirmationChatId(
      NotificationContainer notificationContainer, AppConfig config) {
    if (notificationContainer.getLang() == PortalNotificationLang.EN) {
      return Optional.of(config.getDiscord().getEnConfirmationTournamentChannelId());
    }
    if (notificationContainer.getLang() == PortalNotificationLang.RU) {
      return Optional.of(config.getDiscord().getRuConfirmationTournamentChannelId());
    }
    return Optional.empty();
  }
}
