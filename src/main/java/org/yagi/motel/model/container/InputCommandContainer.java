package org.yagi.motel.model.container;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@SuppressWarnings("checkstyle:MissingJavadocType")
public class InputCommandContainer {
  private Long senderChatId;
  private String messageValue;
  private String username;
  private Map<String, Object> context;
}
