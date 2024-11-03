package org.yagi.motel.handler.holder;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.Data;
import org.yagi.motel.handler.context.HandlerErrorContext;

@Data
@Builder
@SuppressWarnings("checkstyle:MissingJavadocType")
public class PlatformCallbacksHolder {
  private Function<HandlerErrorContext, Optional<String>> platformErrorMessageSupplier;
  private Supplier<Long> platformAdminChatIdSupplier;
}
