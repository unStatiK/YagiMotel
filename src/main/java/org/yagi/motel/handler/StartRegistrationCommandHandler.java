package org.yagi.motel.handler;

import akka.actor.ActorRef;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.yagi.motel.config.AppConfig;
import org.yagi.motel.handler.context.CommandContext;
import org.yagi.motel.handler.enums.ErrorType;
import org.yagi.motel.handler.holder.PlatformCallbacksHolder;
import org.yagi.motel.kernel.enums.CommandType;
import org.yagi.motel.kernel.message.InputCommandMessage;
import org.yagi.motel.kernel.model.container.InputCommandContainer;

@SuppressWarnings("checkstyle:MissingJavadocType")
public class StartRegistrationCommandHandler extends BaseHandler implements CommandHandler {

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public StartRegistrationCommandHandler(
      AppConfig config,
      ActorRef commandDispatcherActor,
      ActorRef errorCommandDispatcherActor,
      PlatformCallbacksHolder platformCallbacksHolder,
      Set<Long> allowedChatIds) {
    super(
        config,
        commandDispatcherActor,
        errorCommandDispatcherActor,
        platformCallbacksHolder,
        allowedChatIds);
  }

  @Override
  public void handleCommand(final CommandContext context) {
    if (!checkPermission(context)) {
      sendErrorReply(context, ErrorType.COMMAND_NOT_ALLOWED);
      return;
    }

    String[] commandArgs = context.getCommandArgs();
    if (commandArgs.length >= 1) {
      if (!StringUtils.isEmpty(context.getUsername())) {
        getCommandDispatcherActor()
            .tell(
                InputCommandMessage.builder()
                    .messageUniqueId(context.getCommandUniqueId())
                    .type(getType())
                    .payload(
                        InputCommandContainer.builder()
                            .messageValue("Регистрация открыта!")
                            .senderChatId(context.getSenderChatId())
                            .build())
                    .platformType(context.getPlatformType())
                    .requestedResponseLang(context.getRequestedResponseLang())
                    .build(),
                ActorRef.noSender());

      } else {
        sendErrorReply(context, ErrorType.MISSED_USERNAME);
      }
    }
  }

  @Override
  public boolean checkPermission(CommandContext context) {
    return super.checkPermission(context);
  }

  @Override
  public CommandType getType() {
    return CommandType.START_REGISTRATION;
  }
}
