package org.yagi.motel.bot.handler;

import akka.actor.ActorRef;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.yagi.motel.bot.CommandContext;
import org.yagi.motel.bot.CommandType;
import org.yagi.motel.config.AppConfig;
import org.yagi.motel.message.InputCommandMessage;
import org.yagi.motel.model.container.InputCommandContainer;

import java.util.function.Function;

public class StatusCommandHandler extends BaseHandler implements CommandHandler {

    public StatusCommandHandler(AppConfig config,
                                ActorRef commandDispatcherActor,
                                Function<SendMessage, Void> tgSendMessageExecuteCallback) {
        super(config, commandDispatcherActor, tgSendMessageExecuteCallback);
    }

    @Override
    public void handleCommand(final CommandContext context) {
        String[] commandArgs = context.getCommandArgs();
        if (commandArgs.length >= 1) {
            getCommandDispatcherActor().tell(InputCommandMessage.builder()
                            .type(getType())
                            .payload(InputCommandContainer.builder()
                                    .messageValue("")
                                    .senderChatId(context.getSenderChatId())
                                    .build())
                            .build(),
                    ActorRef.noSender());
        }
    }

    @Override
    public CommandType getType() {
        return CommandType.STATUS;
    }
}
