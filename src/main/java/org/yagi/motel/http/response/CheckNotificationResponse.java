package org.yagi.motel.http.response;

import java.util.List;
import lombok.Data;

@Data
@SuppressWarnings("checkstyle:MissingJavadocType")
public class CheckNotificationResponse {
  private List<Notification> notifications;
}
