package org.yagi.motel.model.container;

import lombok.Builder;
import lombok.Data;
import org.yagi.motel.bot.ReplyType;

@Data
@Builder
public class ResultCommandContainer {
    private Long replyChatId;
    private ReplyType replyType;
    private String resultMessage;
}
