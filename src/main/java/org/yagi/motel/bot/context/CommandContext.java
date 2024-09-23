package org.yagi.motel.bot.context;

import java.util.function.Consumer;
import lombok.Builder;
import lombok.Getter;
import org.yagi.motel.bot.PlatformType;

@Getter
@Builder
@SuppressWarnings("checkstyle:MissingJavadocType")
public class CommandContext {
  private String[] commandArgs;
  private Long senderChatId;
  private String username;
  private PlatformType platformType;
  private Consumer<String> replyCallback;
  private String requestedResponseLang;
}
