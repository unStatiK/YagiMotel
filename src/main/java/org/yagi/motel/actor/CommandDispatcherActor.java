package org.yagi.motel.actor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import lombok.extern.slf4j.Slf4j;
import org.yagi.motel.bot.ReplyType;
import org.yagi.motel.message.InputCommandMessage;
import org.yagi.motel.model.container.ResultCommandContainer;
import org.yagi.motel.model.holder.CommandDispatchersHolder;

import java.util.concurrent.BlockingQueue;

@Slf4j
public class CommandDispatcherActor extends AbstractActor {

    public static String ACTOR_NAME = "command-dispatcher-actor";

    private final CommandDispatchersHolder commandDispatchersHolder;
    private final BlockingQueue<ResultCommandContainer> commandResultsQueue;

    public CommandDispatcherActor(CommandDispatchersHolder commandDispatchersHolder,
                                  BlockingQueue<ResultCommandContainer> commandResultsQueue) {
        this.commandDispatchersHolder = commandDispatchersHolder;
        this.commandResultsQueue = commandResultsQueue;
    }

    public static Props props(CommandDispatchersHolder commandDispatchersHolder,
                              BlockingQueue<ResultCommandContainer> commandResultsQueue) {
        return Props.create(CommandDispatcherActor.class, commandDispatchersHolder, commandResultsQueue);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(
                        InputCommandMessage.class,
                        message -> {
                            if (message.getType() != null) {
                                switch (message.getType()) {
                                    case LOG:
                                        commandDispatchersHolder.getLogCommandDispatcherActor().tell(message, getSelf());
                                        break;
                                    case ME:
                                        commandDispatchersHolder.getMeCommandDispatcherActor().tell(message, getSelf());
                                        break;
                                    case ADD:
                                        commandDispatchersHolder.getAddCommandDispatcherActor().tell(message, getSelf());
                                        break;
                                    case START_SERVE:
                                        commandResultsQueue.put(ResultCommandContainer.builder()
                                                .replyType(ReplyType.ENABLE_UPDATE_PROCESSING)
                                                .resultMessage(message.getPayload().getMessageValue())
                                                .replyChatId(message.getPayload().getSenderChatId())
                                                .build());
                                        break;
                                    case STOP_SERVE:
                                        commandResultsQueue.put(ResultCommandContainer.builder()
                                                .replyType(ReplyType.DISABLE_UPDATE_PROCESSING)
                                                .resultMessage(message.getPayload().getMessageValue())
                                                .replyChatId(message.getPayload().getSenderChatId())
                                                .build());
                                        break;
                                    case START_REGISTRATION:
                                        commandDispatchersHolder.getStartRegistrationCommandDispatcherActor().tell(message, getSelf());
                                        break;
                                    case CLOSE_REGISTRATION:
                                        commandDispatchersHolder.getCloseRegistrationCommandDispatcherActor().tell(message, getSelf());
                                        break;
                                    case STATUS:
                                        commandDispatchersHolder.getStatusCommandDispatcherActor().tell(message, getSelf());
                                        break;
                                    default:
                                        break;
                                }
                            } else {
                                log.warn("receive untyped message!");
                            }
                        })
                .matchAny(o -> log.warn("received unknown message"))
                .build();
    }
}
