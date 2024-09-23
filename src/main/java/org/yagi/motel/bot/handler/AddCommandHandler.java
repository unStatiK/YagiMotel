package org.yagi.motel.bot.handler;

import akka.actor.ActorRef;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.yagi.motel.bot.CommandType;
import org.yagi.motel.bot.context.CommandContext;
import org.yagi.motel.bot.holder.PlatformCallbacksHolder;
import org.yagi.motel.config.AppConfig;
import org.yagi.motel.message.InputCommandMessage;
import org.yagi.motel.model.container.InputCommandContainer;

@SuppressWarnings("checkstyle:MissingJavadocType")
public class AddCommandHandler extends BaseHandler implements CommandHandler {

  public static final String MS_NICKNAME_CONTEXT_KEY = "ms_nickname";
  public static final String MS_FRIEND_ID_CONTEXT_KEY = "ms_friend_id";

  public AddCommandHandler(
      AppConfig config,
      ActorRef commandDispatcherActor,
      PlatformCallbacksHolder platformCallbacksHolder,
      Set<Long> allowedChatIds) {
    super(config, commandDispatcherActor, platformCallbacksHolder, allowedChatIds);
  }

  @Override
  public void handleCommand(final CommandContext context) {
    if (!checkPermission(context)) {
      return;
    }

    String[] commandArgs = context.getCommandArgs();

    /// add tg_nickname ms_nickname ms_friend_id
    if (commandArgs.length >= 4) {
      String telegramUsername = StringUtils.normalizeSpace(commandArgs[1]);
      String msNicknameUsername = StringUtils.normalizeSpace(commandArgs[2]);
      Long msFriendId = Long.valueOf(commandArgs[3]);

      Map<String, Object> addCommandContext = new HashMap<>();
      addCommandContext.put(MS_NICKNAME_CONTEXT_KEY, msNicknameUsername);
      addCommandContext.put(MS_FRIEND_ID_CONTEXT_KEY, msFriendId);

      getCommandDispatcherActor()
          .tell(
              InputCommandMessage.builder()
                  .type(getType())
                  .payload(
                      InputCommandContainer.builder()
                          .messageValue("")
                          .context(addCommandContext)
                          .senderChatId(context.getSenderChatId())
                          .username(telegramUsername)
                          .build())
                  .platformType(context.getPlatformType())
                  .requestedResponseLang(context.getRequestedResponseLang())
                  .build(),
              ActorRef.noSender());
    }
  }

  @Override
  public boolean checkPermission(CommandContext context) {
    return super.checkPermission(context);
  }

  @Override
  public CommandType getType() {
    return CommandType.ADD;
  }
}
