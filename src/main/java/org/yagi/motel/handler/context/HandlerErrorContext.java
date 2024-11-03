package org.yagi.motel.handler.context;

import lombok.Builder;
import lombok.Data;
import org.yagi.motel.handler.enums.ErrorType;

@Data
@Builder
@SuppressWarnings("checkstyle:MissingJavadocType")
public class HandlerErrorContext {
  private CommandContext context;
  private ErrorType errorType;
}
