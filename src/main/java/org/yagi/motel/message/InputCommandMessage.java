package org.yagi.motel.message;

import java.util.function.Consumer;
import lombok.Builder;
import lombok.Data;
import org.yagi.motel.bot.CommandType;
import org.yagi.motel.bot.PlatformType;
import org.yagi.motel.model.container.InputCommandContainer;

@Data
@Builder
@SuppressWarnings("checkstyle:MissingJavadocType")
public class InputCommandMessage {
  private CommandType type;
  private PlatformType platformType;
  private InputCommandContainer payload;
  private Consumer<String> replyCallBack;
  private String requestedResponseLang;
}
