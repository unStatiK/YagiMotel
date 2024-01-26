package org.yagi.motel.message;

import lombok.Builder;
import lombok.Data;
import org.yagi.motel.bot.CommandType;
import org.yagi.motel.model.container.InputCommandContainer;

@Data
@Builder
public class InputCommandMessage {
    private CommandType type;
    private InputCommandContainer payload;
}
