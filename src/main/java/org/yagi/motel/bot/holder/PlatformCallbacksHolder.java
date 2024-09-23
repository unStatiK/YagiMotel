package org.yagi.motel.bot.holder;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.Data;
import org.yagi.motel.bot.context.HandlerErrorContext;
import org.yagi.motel.bot.context.ReplyCallbackContext;

@Data
@Builder
@SuppressWarnings("checkstyle:MissingJavadocType")
public class PlatformCallbacksHolder {
  private Consumer<Optional<ReplyCallbackContext>> platformSendMessageCallback;
  private Function<HandlerErrorContext, Optional<ReplyCallbackContext>> platformErrorReplySupplier;
  private Supplier<Long> platformAdminChatIdSupplier;
}
