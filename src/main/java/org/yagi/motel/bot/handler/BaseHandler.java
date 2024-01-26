package org.yagi.motel.bot.handler;

import akka.actor.ActorRef;
import lombok.Getter;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.yagi.motel.config.AppConfig;

import java.util.function.Function;

public abstract class BaseHandler {

    @Getter
    private final Function<SendMessage, Void> tgSendMessageExecuteCallback;
    @Getter
    private final ActorRef commandDispatcherActor;
    @Getter
    private final AppConfig config;

    public BaseHandler(AppConfig config, ActorRef commandDispatcherActor, Function<SendMessage, Void> tgSendMessageExecuteCallback) {
        this.config = config;
        this.commandDispatcherActor = commandDispatcherActor;
        this.tgSendMessageExecuteCallback = tgSendMessageExecuteCallback;
    }

}
