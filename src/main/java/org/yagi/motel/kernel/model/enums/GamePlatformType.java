package org.yagi.motel.kernel.model.enums;

import java.util.Locale;

@SuppressWarnings("checkstyle:MissingJavadocType")
public enum GamePlatformType {
  TENHOU,
  MAJSOUL;

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static GamePlatformType fromString(String platform) {
    try {
      return valueOf(platform.toUpperCase(Locale.US));
    } catch (Exception ex) {
      return TENHOU;
    }
  }
}
