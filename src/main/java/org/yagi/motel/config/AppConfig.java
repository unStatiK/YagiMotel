package org.yagi.motel.config;

import lombok.Data;
import org.yagi.motel.config.properties.DiscordProperties;
import org.yagi.motel.config.properties.TelegramProperties;

@Data
@SuppressWarnings("checkstyle:MissingJavadocType")
public class AppConfig {
  private TelegramProperties telegram;
  private DiscordProperties discord;
  private Integer commandResultQueueCapacity;
  private Integer notificationQueueCapacity;
  private Integer eventsMailboxPool;
  private Integer commandResultHandlerThreadNumber;
  private Integer notificationHandlerThreadNumber;
  private Boolean disableOnStart;
  private Long checkNotificationsIntervalInSeconds;
  private String tensoulAppToken;
  private String tensoulUrl;
  private String portalUrl;
  private String autobotApiToken;
  private Long tournamentId;
  private Long lobbyId;
  private String gamePlatform;
}
