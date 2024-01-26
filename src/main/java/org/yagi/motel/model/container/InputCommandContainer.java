package org.yagi.motel.model.container;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class InputCommandContainer {
    private Long senderChatId;
    private String messageValue;
    private String telegramUsername;
    private Map<String, Object> context;
}
