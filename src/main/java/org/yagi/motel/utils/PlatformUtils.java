package org.yagi.motel.utils;

import lombok.experimental.UtilityClass;
import org.yagi.motel.bot.PlatformType;
import org.yagi.motel.config.AppConfig;

@UtilityClass
@SuppressWarnings("checkstyle:MissingJavadocType")
public class PlatformUtils {
  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static boolean isPlatformEnable(AppConfig config, PlatformType platformType) {
    if (PlatformType.TG == platformType && Boolean.TRUE.equals(config.getTelegram().getEnable())) {
      return true;
    }
    if (PlatformType.DISCORD == platformType
        && Boolean.TRUE.equals(config.getDiscord().getEnable())) {
      return true;
    }
    return false;
  }
}
