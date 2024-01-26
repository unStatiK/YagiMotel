package org.yagi.motel.bot.handler;

import akka.actor.ActorRef;
import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.yagi.motel.bot.CommandContext;
import org.yagi.motel.bot.CommandType;
import org.yagi.motel.config.AppConfig;
import org.yagi.motel.message.InputCommandMessage;
import org.yagi.motel.model.container.InputCommandContainer;
import org.yagi.motel.model.enums.GamePlatformType;

import java.util.function.Function;

public class MeCommandHandler extends BaseHandler implements CommandHandler {

    public MeCommandHandler(AppConfig config,
                            ActorRef commandDispatcherActor,
                            Function<SendMessage, Void> tgSendMessageExecuteCallback) {
        super(config, commandDispatcherActor, tgSendMessageExecuteCallback);
    }

    @Override
    public void handleCommand(final CommandContext context) {
        if (StringUtils.isEmpty(context.getTelegramUsername())) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(context.getSenderChatId());
            sendMessage.setText(String.format("Перед привязкой %s ника нужно установить username в настройках " +
                            "телеграма. Инструкция: http://telegramzy.ru/nik-v-telegramm/",
                    GamePlatformType.fromString(getConfig().getGamePlatform()) == GamePlatformType.MAJSOUL ? "mahjongsoul" : "tenhou.net"));
            getTgSendMessageExecuteCallback().apply(sendMessage);
            return;
        }

        String[] commandArgs = context.getCommandArgs();
        if (commandArgs.length >= 2) {
            String playerUsername = StringUtils.normalizeSpace(commandArgs[1]);
            getCommandDispatcherActor().tell(InputCommandMessage.builder()
                            .type(getType())
                            .payload(InputCommandContainer.builder()
                                    .messageValue(playerUsername)
                                    .senderChatId(context.getSenderChatId())
                                    .telegramUsername(context.getTelegramUsername())
                                    .build())
                            .build(),
                    ActorRef.noSender());
        } else {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(context.getSenderChatId());
            sendMessage.setText(String.format("@%s Укажите ваш %s nickname после команды.", context.getTelegramUsername(),
                    GamePlatformType.fromString(getConfig().getGamePlatform()) == GamePlatformType.MAJSOUL ? "mahjongsoul" : "tenhou.net"));
            getTgSendMessageExecuteCallback().apply(sendMessage);
        }
    }

    @Override
    public CommandType getType() {
        return CommandType.ME;
    }
}
