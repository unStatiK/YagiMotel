package org.yagi.motel.response;

import lombok.Data;

import java.util.List;

@Data
public class CheckNotificationResponse {
    private List<Notification> notifications;
}
