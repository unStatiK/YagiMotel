package org.yagi.motel.bot.handler;

import akka.actor.ActorRef;
import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.yagi.motel.bot.CommandContext;
import org.yagi.motel.bot.CommandType;
import org.yagi.motel.config.AppConfig;
import org.yagi.motel.message.InputCommandMessage;
import org.yagi.motel.model.container.InputCommandContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class AddCommandHandler extends BaseHandler implements CommandHandler {

    public static final String MS_NICKNAME_CONTEXT_KEY = "ms_nickname";
    public static final String MS_FRIEND_ID_CONTEXT_KEY = "ms_friend_id";

    public AddCommandHandler(AppConfig config,
                             ActorRef commandDispatcherActor,
                             Function<SendMessage, Void> tgSendMessageExecuteCallback) {
        super(config, commandDispatcherActor, tgSendMessageExecuteCallback);
    }

    @Override
    public void handleCommand(final CommandContext context) {
        String[] commandArgs = context.getCommandArgs();

        ///add tg_nickname ms_nickname ms_friend_id
        if (commandArgs.length >= 4) {
            if (context.getSenderChatId().equals(getConfig().getAdminChatId())) {
                String telegramUsername = StringUtils.normalizeSpace(commandArgs[1]);
                String msNicknameUsername = StringUtils.normalizeSpace(commandArgs[2]);
                Long msFriendId = Long.valueOf(commandArgs[3]);

                Map<String, Object> addCommandContext = new HashMap<>();
                addCommandContext.put(MS_NICKNAME_CONTEXT_KEY, msNicknameUsername);
                addCommandContext.put(MS_FRIEND_ID_CONTEXT_KEY, msFriendId);

                getCommandDispatcherActor().tell(InputCommandMessage.builder()
                                .type(getType())
                                .payload(InputCommandContainer.builder()
                                        .messageValue("")
                                        .context(addCommandContext)
                                        .senderChatId(getConfig().getTournamentChatId())
                                        .telegramUsername(telegramUsername)
                                        .build())
                                .build(),
                        ActorRef.noSender());
            }
        }
    }

    @Override
    public CommandType getType() {
        return CommandType.ADD;
    }
}
