package org.yagi.motel.bot.handler;

import akka.actor.ActorRef;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import org.yagi.motel.bot.ErrorType;
import org.yagi.motel.bot.context.CommandContext;
import org.yagi.motel.bot.context.HandlerErrorContext;
import org.yagi.motel.bot.context.ReplyCallbackContext;
import org.yagi.motel.bot.holder.PlatformCallbacksHolder;
import org.yagi.motel.config.AppConfig;

@SuppressWarnings("checkstyle:MissingJavadocType")
public abstract class BaseHandler {

  @Getter
  private final PlatformCallbacksHolder platformCallbacksHolder;
  @Getter
  private final ActorRef commandDispatcherActor;
  @Getter
  private final AppConfig config;
  @Getter
  private final Set<Long> allowedChatIds;

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public BaseHandler(
      AppConfig config,
      ActorRef commandDispatcherActor,
      PlatformCallbacksHolder platformCallbacksHolder,
      Set<Long> allowedChatIds) {
    this.config = config;
    this.commandDispatcherActor = commandDispatcherActor;
    this.platformCallbacksHolder = platformCallbacksHolder;
    this.allowedChatIds = allowedChatIds;
  }

  protected Optional<ReplyCallbackContext> getContextWithError(CommandContext context,
                                                               ErrorType errorType) {
    return getPlatformCallbacksHolder()
        .getPlatformErrorReplySupplier()
        .apply(HandlerErrorContext.builder().context(context).errorType(errorType).build());
  }

  protected boolean checkPermission(CommandContext context) {
    if (getAllowedChatIds() == null || getAllowedChatIds().isEmpty()) {
      return true;
    }

    PlatformCallbacksHolder callbacksHolder = getPlatformCallbacksHolder();
    if (!getAllowedChatIds().contains(context.getSenderChatId())) {
      callbacksHolder
          .getPlatformSendMessageCallback()
          .accept(getContextWithError(context, ErrorType.COMMAND_NOT_ALLOWED));
      return false;
    }
    return true;
  }
}
