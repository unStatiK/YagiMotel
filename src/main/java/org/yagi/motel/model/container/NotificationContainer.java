package org.yagi.motel.model.container;

import lombok.Builder;
import lombok.Data;
import org.yagi.motel.bot.NotificationType;

@Data
@Builder
public class NotificationContainer {
    private NotificationType notificationType;
    private String message;
}
