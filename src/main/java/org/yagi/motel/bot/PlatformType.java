package org.yagi.motel.bot;

import java.util.Optional;

@SuppressWarnings("checkstyle:MissingJavadocType")
public enum PlatformType {
  TG(0),
  DISCORD(1);

  private final int platformCode;

  PlatformType(int platformCode) {
    this.platformCode = platformCode;
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static Optional<PlatformType> getPlatformTypeFromCode(int platformCode) {
    for (PlatformType platformType : values()) {
      if (platformType.platformCode == platformCode) {
        return Optional.of(platformType);
      }
    }
    return Optional.empty();
  }
}
