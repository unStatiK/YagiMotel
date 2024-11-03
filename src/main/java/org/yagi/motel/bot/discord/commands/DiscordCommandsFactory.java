package org.yagi.motel.bot.discord.commands;

import discord4j.core.DiscordClient;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.yagi.motel.config.AppConfig;

@SuppressWarnings("checkstyle:MissingJavadocType")
public class DiscordCommandsFactory {
  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static ApplicationCommandRequest createMeCommand(DiscordClient client, AppConfig config) {
    ApplicationCommandRequest greetMeRequest =
        ApplicationCommandRequest.builder()
            .name("me")
            .description("Confirmation of participation in tournament")
            .addOption(
                ApplicationCommandOptionData.builder()
                    .name("nickname")
                    .description("Your game nickname")
                    .type(ApplicationCommandOption.Type.STRING.getValue())
                    .required(true)
                    .build())
            .build();
    client
        .getApplicationService()
        .createGlobalApplicationCommand(config.getDiscord().getApplicationId(), greetMeRequest)
        .block();
    return greetMeRequest;
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static ApplicationCommandRequest createLogCommand(DiscordClient client, AppConfig config) {
    ApplicationCommandRequest greetMeRequest =
        ApplicationCommandRequest.builder()
            .name("log")
            .description("send log to pantheon rating")
            .addOption(
                ApplicationCommandOptionData.builder()
                    .name("log")
                    .description("log link")
                    .type(ApplicationCommandOption.Type.STRING.getValue())
                    .required(true)
                    .build())
            .build();
    client
        .getApplicationService()
        .createGlobalApplicationCommand(config.getDiscord().getApplicationId(), greetMeRequest)
        .block();
    return greetMeRequest;
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static ApplicationCommandRequest createStatusCommand(
      DiscordClient client, AppConfig config) {
    ApplicationCommandRequest greetStatusRequest =
        ApplicationCommandRequest.builder()
            .name("status")
            .description("Get tournament status")
            .build();
    client
        .getApplicationService()
        .createGlobalApplicationCommand(config.getDiscord().getApplicationId(), greetStatusRequest)
        .block();
    return greetStatusRequest;
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static ApplicationCommandRequest createUpdateTeamsCommand(
      DiscordClient client, AppConfig config) {
    ApplicationCommandRequest updateTeamsCommandRequest =
        ApplicationCommandRequest.builder()
            .name("update_teams")
            .description("Send team names to pantheon over portal")
            .build();
    client
        .getApplicationService()
        .createGlobalApplicationCommand(
            config.getDiscord().getApplicationId(), updateTeamsCommandRequest)
        .block();
    return updateTeamsCommandRequest;
  }
}
