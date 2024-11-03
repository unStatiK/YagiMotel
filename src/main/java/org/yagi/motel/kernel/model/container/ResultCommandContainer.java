package org.yagi.motel.kernel.model.container;

import lombok.Builder;
import lombok.Data;
import org.yagi.motel.kernel.enums.PlatformType;

@Data
@Builder
@SuppressWarnings("checkstyle:MissingJavadocType")
public class ResultCommandContainer {
  private Long uniqueMessageId;
  private Long replyChatId;
  private PlatformType platformType;
  private String resultMessage;
}
