package org.yagi.motel.model.container;

import java.util.function.Consumer;
import lombok.Builder;
import lombok.Data;
import org.yagi.motel.bot.PlatformType;
import org.yagi.motel.bot.ReplyType;

@Data
@Builder
@SuppressWarnings("checkstyle:MissingJavadocType")
public class ResultCommandContainer {
  private Long replyChatId;
  private ReplyType replyType;
  private PlatformType platformType;
  private String resultMessage;
  private Consumer<String> replyCallback;
}
