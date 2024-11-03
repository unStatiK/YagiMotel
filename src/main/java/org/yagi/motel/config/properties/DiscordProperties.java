package org.yagi.motel.config.properties;

import lombok.Data;

@Data
@SuppressWarnings("checkstyle:MissingJavadocType")
public class DiscordProperties {
  private Boolean enable;
  private Long applicationId;
  private String discordBotToken;
  private Long discordBotUserId;
  private Long discordAdminChatId;
  private Long ruNotificationTournamentChannelId;
  private Long enNotificationTournamentChannelId;
  private Long ruConfirmationTournamentChannelId;
  private Long enConfirmationTournamentChannelId;
  private Long gameLogsChannelId;
  private Integer messagesThreadNumber;
  private Integer messagesQueueCapacity;
}
