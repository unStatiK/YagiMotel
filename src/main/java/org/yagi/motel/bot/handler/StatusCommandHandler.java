package org.yagi.motel.bot.handler;

import akka.actor.ActorRef;
import java.util.Set;
import org.yagi.motel.bot.CommandType;
import org.yagi.motel.bot.context.CommandContext;
import org.yagi.motel.bot.holder.PlatformCallbacksHolder;
import org.yagi.motel.config.AppConfig;
import org.yagi.motel.message.InputCommandMessage;
import org.yagi.motel.model.container.InputCommandContainer;

@SuppressWarnings("checkstyle:MissingJavadocType")
public class StatusCommandHandler extends BaseHandler implements CommandHandler {

  public StatusCommandHandler(
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
    if (commandArgs.length >= 1) {
      getCommandDispatcherActor()
          .tell(
              InputCommandMessage.builder()
                  .type(getType())
                  .payload(
                      InputCommandContainer.builder()
                          .messageValue("")
                          .senderChatId(context.getSenderChatId())
                          .build())
                  .platformType(context.getPlatformType())
                  .replyCallBack(context.getReplyCallback())
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
    return CommandType.STATUS;
  }
}
