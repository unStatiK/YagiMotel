package org.yagi.motel.config.properties;

import lombok.Data;

@Data
@SuppressWarnings("checkstyle:MissingJavadocType")
public class TelegramProperties {
  private Boolean enable;
  private String tgBotUsername;
  private String tgBotToken;
  private Long tgAdminChatId;
  private Long tournamentChatId;
}
