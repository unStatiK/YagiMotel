package org.yagi.motel.bot.handler;

import akka.actor.ActorRef;
import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.yagi.motel.bot.CommandContext;
import org.yagi.motel.bot.CommandType;
import org.yagi.motel.config.AppConfig;
import org.yagi.motel.message.InputCommandMessage;
import org.yagi.motel.model.container.InputCommandContainer;

import java.util.function.Function;

public class StartServeCommandHandler extends BaseHandler implements CommandHandler {

    public StartServeCommandHandler(AppConfig config,
                                    ActorRef commandDispatcherActor,
                                    Function<SendMessage, Void> tgSendMessageExecuteCallback) {
        super(config, commandDispatcherActor, tgSendMessageExecuteCallback);
    }

    @Override
    public void handleCommand(final CommandContext context) {
        String[] commandArgs = context.getCommandArgs();
        if (commandArgs.length >= 1) {
            if (!StringUtils.isEmpty(context.getTelegramUsername())) {
                if (!context.getSenderChatId().equals(getConfig().getAdminChatId())) {
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(context.getSenderChatId());
                    sendMessage.setText(String.format("@%s эта команда недоступна", context.getTelegramUsername()));
                    getTgSendMessageExecuteCallback().apply(sendMessage);
                } else {
                    getCommandDispatcherActor().tell(InputCommandMessage.builder()
                                    .type(getType())
                                    .payload(InputCommandContainer.builder()
                                            .messageValue("Обработка команд включена!")
                                            .senderChatId(context.getSenderChatId())
                                            .build())
                                    .build(),
                            ActorRef.noSender());
                }
            } else {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(context.getSenderChatId());
                sendMessage.setText("Нужно прописать username в настройках telegram!");
                getTgSendMessageExecuteCallback().apply(sendMessage);
            }
        }
    }

    @Override
    public CommandType getType() {
        return CommandType.START_SERVE;
    }
}
