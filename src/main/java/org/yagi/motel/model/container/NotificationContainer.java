package org.yagi.motel.model.container;

import lombok.Builder;
import lombok.Data;
import org.yagi.motel.bot.NotificationType;
import org.yagi.motel.bot.PlatformType;
import org.yagi.motel.bot.PortalNotificationLang;

@Data
@Builder
@SuppressWarnings("checkstyle:MissingJavadocType")
public class NotificationContainer {
  private NotificationType notificationType;
  private String message;
  private PlatformType platformType;
  private PortalNotificationLang lang;
  private String messageType;
}
