package org.yagi.motel.bot.handler;

import org.yagi.motel.bot.CommandContext;
import org.yagi.motel.bot.CommandType;

public interface CommandHandler {
    void handleCommand(CommandContext context);
    CommandType getType();
}
