package org.yagi.motel.bot.handler;

import org.yagi.motel.bot.CommandType;
import org.yagi.motel.bot.context.CommandContext;

@SuppressWarnings("checkstyle:MissingJavadocType")
public interface CommandHandler {
  void handleCommand(CommandContext context);

  boolean checkPermission(CommandContext context);

  CommandType getType();
}
