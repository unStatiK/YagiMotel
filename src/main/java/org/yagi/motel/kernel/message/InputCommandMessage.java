package org.yagi.motel.kernel.message;

import lombok.Builder;
import lombok.Data;
import org.yagi.motel.kernel.enums.CommandType;
import org.yagi.motel.kernel.enums.PlatformType;
import org.yagi.motel.kernel.model.container.InputCommandContainer;

@Data
@Builder
@SuppressWarnings("checkstyle:MissingJavadocType")
public class InputCommandMessage {
  private Long messageUniqueId;
  private CommandType type;
  private PlatformType platformType;
  private InputCommandContainer payload;
  private String requestedResponseLang;
}
