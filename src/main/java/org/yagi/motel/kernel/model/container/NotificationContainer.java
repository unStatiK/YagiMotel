package org.yagi.motel.kernel.model.container;

import lombok.Builder;
import lombok.Data;
import org.yagi.motel.kernel.enums.NotificationType;
import org.yagi.motel.kernel.enums.PlatformType;
import org.yagi.motel.kernel.enums.PortalNotificationLang;

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
