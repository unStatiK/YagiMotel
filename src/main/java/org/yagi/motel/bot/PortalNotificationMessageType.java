package org.yagi.motel.bot;

import lombok.Getter;

@SuppressWarnings("checkstyle:MissingJavadocType")
public enum PortalNotificationMessageType {
  CONFIRMATION_STARTED("confirmation_started");

  @Getter private final String messageType;

  PortalNotificationMessageType(String messageType) {
    this.messageType = messageType;
  }
}
