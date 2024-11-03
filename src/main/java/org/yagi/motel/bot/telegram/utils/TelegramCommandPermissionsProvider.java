package org.yagi.motel.bot.telegram.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.yagi.motel.config.AppConfig;

@UtilityClass
@SuppressWarnings("checkstyle:MissingJavadocType")
public class TelegramCommandPermissionsProvider {
  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static Set<Long> getStartServeCommandPermissions(AppConfig config) {
    return getIds(config.getTelegram().getTgAdminChatId());
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static Set<Long> getStopServeCommandPermissions(AppConfig config) {
    return getIds(config.getTelegram().getTgAdminChatId());
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static Set<Long> getStartRegistrationCommandPermissions(AppConfig config) {
    return getIds(config.getTelegram().getTgAdminChatId());
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static Set<Long> getCloseRegistrationCommandPermissions(AppConfig config) {
    return getIds(config.getTelegram().getTgAdminChatId());
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static Set<Long> getMeCommandPermissions(AppConfig config) {
    return getIds(config.getTelegram().getTournamentChatId());
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static Set<Long> getAddCommandPermissions(AppConfig config) {
    return getIds(config.getTelegram().getTgAdminChatId());
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static Set<Long> getUpdateTeamsCommandPermissions(AppConfig config) {
    return getIds(config.getTelegram().getTgAdminChatId());
  }

  private Set<Long> getIds(Long... ids) {
    return new HashSet<>(Arrays.asList(ids));
  }
}
