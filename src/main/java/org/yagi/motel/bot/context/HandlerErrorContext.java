package org.yagi.motel.bot.context;

import lombok.Builder;
import lombok.Data;
import org.yagi.motel.bot.ErrorType;

@Data
@Builder
@SuppressWarnings("checkstyle:MissingJavadocType")
public class HandlerErrorContext {
  private CommandContext context;
  private ErrorType errorType;
}
