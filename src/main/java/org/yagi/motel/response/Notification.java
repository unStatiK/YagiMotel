package org.yagi.motel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Notification {
    private String message;
    @JsonProperty("notification_id")
    private Long notificationId;
}
