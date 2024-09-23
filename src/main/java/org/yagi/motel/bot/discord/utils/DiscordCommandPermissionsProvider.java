package org.yagi.motel.bot.discord.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.yagi.motel.config.AppConfig;

@UtilityClass
@SuppressWarnings("checkstyle:MissingJavadocType")
public class DiscordCommandPermissionsProvider {
  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static Set<Long> getStatusCommandPermissions(AppConfig config) {
    return getIds(config.getDiscord().getRuNotificationTournamentChannelId(),
        config.getDiscord().getEnNotificationTournamentChannelId(),
        config.getDiscord().getDiscordAdminChatId());
  }

  public static Set<Long> getMeCommandPermissions(AppConfig config) {
    return getIds(config.getDiscord().getRuConfirmationTournamentChannelId(),
        config.getDiscord().getEnConfirmationTournamentChannelId());
  }

  public static Set<Long> getStartServeCommandPermissions(AppConfig config) {
    return getIds(config.getDiscord().getDiscordAdminChatId());
  }

  public static Set<Long> getStopServeCommandPermissions(AppConfig config) {
    return getIds(config.getDiscord().getDiscordAdminChatId());
  }

  public static Set<Long> getStartRegistrationCommandPermissions(AppConfig config) {
    return getIds(config.getDiscord().getDiscordAdminChatId());
  }

  public static Set<Long> getCloseRegistrationCommandPermissions(AppConfig config) {
    return getIds(config.getDiscord().getDiscordAdminChatId());
  }

  public static Set<Long> getLogCommandPermissions(AppConfig config) {
    return getIds(config.getDiscord().getGameLogsChannelId());
  }

  public static Set<Long> getAddCommandPermissions(AppConfig config) {
    return getIds(config.getDiscord().getDiscordAdminChatId());
  }

  private Set<Long> getIds(Long... ids) {
    return new HashSet<>(Arrays.asList(ids));
  }
}
