package org.yagi.motel.http.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@SuppressWarnings("checkstyle:MissingJavadocType")
public class Notification {
  private String message;

  @JsonProperty("notification_id")
  private Long notificationId;

  private short destination;

  private String lang;

  private String type;
}
