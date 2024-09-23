package org.yagi.motel.utils;

import lombok.experimental.UtilityClass;
import org.yagi.motel.bot.ReplyType;
import org.yagi.motel.message.InputCommandMessage;

@UtilityClass
@SuppressWarnings("checkstyle:MissingJavadocType")
public class ReplyTypeUtils {
  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static ReplyType getReplyType(final InputCommandMessage message) {
    return message.getReplyCallBack() != null
        ? ReplyType.CALLBACK
        : ReplyType.SEND_MESSAGE;
  }
}
