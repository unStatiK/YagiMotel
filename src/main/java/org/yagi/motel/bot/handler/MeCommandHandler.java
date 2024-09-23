package org.yagi.motel.bot.handler;

import akka.actor.ActorRef;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.yagi.motel.bot.CommandType;
import org.yagi.motel.bot.ErrorType;
import org.yagi.motel.bot.context.CommandContext;
import org.yagi.motel.bot.holder.PlatformCallbacksHolder;
import org.yagi.motel.config.AppConfig;
import org.yagi.motel.message.InputCommandMessage;
import org.yagi.motel.model.container.InputCommandContainer;

@SuppressWarnings("checkstyle:MissingJavadocType")
public class MeCommandHandler extends BaseHandler implements CommandHandler {

  public MeCommandHandler(
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

    PlatformCallbacksHolder callbacksHolder = getPlatformCallbacksHolder();
    if (StringUtils.isEmpty(context.getUsername())) {
      callbacksHolder
          .getPlatformSendMessageCallback()
          .accept(getContextWithError(context, ErrorType.MISSED_PLATFORM_USERNAME));
      return;
    }

    String[] commandArgs = context.getCommandArgs();
    if (commandArgs.length >= 2) {
      String playerUsername = StringUtils.normalizeSpace(commandArgs[1]);
      getCommandDispatcherActor()
          .tell(
              InputCommandMessage.builder()
                  .type(getType())
                  .payload(
                      InputCommandContainer.builder()
                          .messageValue(playerUsername)
                          .senderChatId(context.getSenderChatId())
                          .username(context.getUsername())
                          .build())
                  .platformType(context.getPlatformType())
                  .replyCallBack(context.getReplyCallback())
                  .requestedResponseLang(context.getRequestedResponseLang())
                  .build(),
              ActorRef.noSender());
    } else {
      callbacksHolder
          .getPlatformSendMessageCallback()
          .accept(getContextWithError(context, ErrorType.MISSED_USERNAME));
    }
  }

  @Override
  public boolean checkPermission(CommandContext context) {
    return super.checkPermission(context);
  }

  @Override
  public CommandType getType() {
    return CommandType.ME;
  }
}
