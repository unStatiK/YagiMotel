package org.yagi.motel.bot.discord.utils;

import discord4j.core.object.command.Interaction;
import discord4j.discordjson.json.ApplicationCommandInteractionData;
import discord4j.discordjson.json.ApplicationCommandInteractionOptionData;
import java.util.List;
import java.util.Optional;
import lombok.experimental.UtilityClass;

@UtilityClass
@SuppressWarnings("checkstyle:MissingJavadocType")
public class DiscordInteractionUtils {

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static String[] extractArgsFromInteraction(Interaction interaction) {
    String[] args = new String[] {};
    if (interaction != null && !interaction.getData().data().isAbsent()) {
      ApplicationCommandInteractionData currentInteractionData = interaction.getData().data().get();
      if (!currentInteractionData.options().isAbsent()) {
        List<ApplicationCommandInteractionOptionData> currentInteractionOptions =
            currentInteractionData.options().get();
        args =
            currentInteractionOptions.stream()
                .filter(option -> !option.value().isAbsent())
                .map(option -> option.value().get())
                .toArray(String[]::new);
      }
    }
    return args;
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static Optional<String> extractInteractionName(Interaction interaction) {
    if (interaction != null && !interaction.getData().data().isAbsent()) {
      ApplicationCommandInteractionData currentInteractionData = interaction.getData().data().get();
      if (!currentInteractionData.name().isAbsent()) {
        return Optional.of(currentInteractionData.name().get());
      }
    }
    return Optional.empty();
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static String[] prepareHandlerArgs(String commandPrefix, String[] commandArgs) {
    String[] args = new String[commandArgs.length + 1];
    args[0] = commandPrefix;
    int index = 1;
    for (final String commandArg : commandArgs) {
      args[index] = commandArg;
      index++;
    }
    return args;
  }
}
