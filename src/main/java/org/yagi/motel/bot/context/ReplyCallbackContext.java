package org.yagi.motel.bot.context;

import java.util.function.Consumer;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@SuppressWarnings("checkstyle:MissingJavadocType")
public class ReplyCallbackContext {
  private Long targetChatId;
  private String text;
  private Consumer<String> replyCallback;
}
