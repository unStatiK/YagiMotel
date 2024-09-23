package org.yagi.motel.response;

import java.util.List;
import lombok.Data;

@Data
@SuppressWarnings("checkstyle:MissingJavadocType")
public class CheckNotificationResponse {
  private List<Notification> notifications;
}
