package org.yagi.motel.config;

import lombok.Data;

@Data
public class AppConfig {
    private String botUsername;
    private String botToken;
    private Integer commandResultQueueCapacity;
    private Integer notificationQueueCapacity;
    private Integer eventsMailboxPool;
    private Integer commandResultHandlerThreadNumber;
    private Integer notificationHandlerThreadNumber;
    private Boolean disableOnStart;
    private Long adminChatId;
    private Long checkNotificationsIntervalInSeconds;
    private String tensoulAppToken;
    private String tensoulUrl;
    private String portalUrl;
    private String autobotApiToken;
    private Long tournamentId;
    private Long lobbyId;
    private Long tournamentChatId;
    private String gamePlatform;
}
