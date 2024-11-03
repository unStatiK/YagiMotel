package org.yagi.motel.handler;

import akka.actor.ActorRef;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import org.yagi.motel.config.AppConfig;
import org.yagi.motel.handler.context.CommandContext;
import org.yagi.motel.handler.context.HandlerErrorContext;
import org.yagi.motel.handler.enums.ErrorType;
import org.yagi.motel.handler.holder.PlatformCallbacksHolder;
import org.yagi.motel.kernel.model.container.ResultCommandContainer;

@SuppressWarnings("checkstyle:MissingJavadocType")
public abstract class BaseHandler {

  @Getter private final PlatformCallbacksHolder platformCallbacksHolder;
  @Getter private final ActorRef commandDispatcherActor;
  @Getter private final ActorRef errorCommandDispatcherActor;
  @Getter private final AppConfig config;
  @Getter private final Set<Long> allowedChatIds;

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public BaseHandler(
      AppConfig config,
      ActorRef commandDispatcherActor,
      ActorRef errorCommandDispatcherActor,
      PlatformCallbacksHolder platformCallbacksHolder,
      Set<Long> allowedChatIds) {
    this.config = config;
    this.commandDispatcherActor = commandDispatcherActor;
    this.errorCommandDispatcherActor = errorCommandDispatcherActor;
    this.platformCallbacksHolder = platformCallbacksHolder;
    this.allowedChatIds = allowedChatIds;
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  protected Optional<String> getErrorMessage(CommandContext context, ErrorType errorType) {
    return getPlatformCallbacksHolder()
        .getPlatformErrorMessageSupplier()
        .apply(HandlerErrorContext.builder().context(context).errorType(errorType).build());
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  protected boolean checkPermission(CommandContext context) {
    if (getAllowedChatIds() == null || getAllowedChatIds().isEmpty()) {
      return true;
    }
    if (!getAllowedChatIds().contains(context.getSenderChatId())) {
      return false;
    }
    return true;
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  protected void sendErrorReply(CommandContext context, ErrorType errorType) {
    Optional<String> resultMessage = getErrorMessage(context, errorType);
    resultMessage.ifPresent(
        msg ->
            getErrorCommandDispatcherActor()
                .tell(
                    ResultCommandContainer.builder()
                        .uniqueMessageId(context.getCommandUniqueId())
                        .replyChatId(context.getSenderChatId())
                        .platformType(context.getPlatformType())
                        .resultMessage(msg)
                        .build(),
                    ActorRef.noSender()));
  }
}
