package org.yagi.motel.bot;

import lombok.Builder;
import lombok.Getter;
import org.telegram.telegrambots.meta.api.objects.Update;

@Getter
@Builder
public class CommandContext {
    private Update update;
    private String[] commandArgs;
    private Long senderChatId;
    private String telegramUsername;
}
