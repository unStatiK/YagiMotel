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
public class LogCommandHandler extends BaseHandler implements CommandHandler {

  public LogCommandHandler(
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
    if (commandArgs.length >= 2) {
      PlatformCallbacksHolder callbacksHolder = getPlatformCallbacksHolder();
      boolean isTenhouLog = commandArgs[1].contains("tenhou.net");
      if (isTenhouLog || context.getSenderChatId()
          .equals(callbacksHolder.getPlatformAdminChatIdSupplier().get())) {
        String commandValue = commandArgs[1];
        getCommandDispatcherActor()
            .tell(
                InputCommandMessage.builder()
                    .type(getType())
                    .payload(
                        InputCommandContainer.builder()
                            .messageValue(commandValue)
                            .senderChatId(context.getSenderChatId())
                            .build())
                    .platformType(context.getPlatformType())
                    .requestedResponseLang(context.getRequestedResponseLang())
                    .replyCallBack(context.getReplyCallback())
                    .build(),
                ActorRef.noSender());
      }
    }
  }

  @Override
  public boolean checkPermission(CommandContext context) {
    return super.checkPermission(context);
  }

  @Override
  public CommandType getType() {
    return CommandType.LOG;
  }
}
