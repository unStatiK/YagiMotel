package org.yagi.motel.model.container;

import lombok.Builder;
import lombok.Data;
import org.yagi.motel.bot.discord.DiscordTournamentHelper;
import org.yagi.motel.bot.telegram.TournamentHelper;

@Data
@Builder
@SuppressWarnings("checkstyle:MissingJavadocType")
public class CommunicationPlatformsContainer {
  private TournamentHelper tgBot;
  private DiscordTournamentHelper discordBot;
}
