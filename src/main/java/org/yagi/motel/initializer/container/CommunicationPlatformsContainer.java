package org.yagi.motel.initializer.container;

import lombok.Builder;
import lombok.Data;
import org.yagi.motel.bot.discord.DiscordTournamentHelper;
import org.yagi.motel.bot.telegram.TgTournamentHelper;

@Data
@Builder
@SuppressWarnings("checkstyle:MissingJavadocType")
public class CommunicationPlatformsContainer {
  private TgTournamentHelper tgBot;
  private DiscordTournamentHelper discordBot;
}
