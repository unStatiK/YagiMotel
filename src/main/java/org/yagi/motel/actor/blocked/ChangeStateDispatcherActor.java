package org.yagi.motel.actor.blocked;

import static org.yagi.motel.utils.ReplyTypeUtils.getReplyType;

import akka.actor.AbstractActor;
import akka.actor.Props;
import java.util.concurrent.BlockingQueue;
import lombok.extern.slf4j.Slf4j;
import org.yagi.motel.message.InputCommandMessage;
import org.yagi.motel.model.container.ResultCommandContainer;
import org.yagi.motel.model.enums.IsProcessedState;
import org.yagi.motel.repository.StateRepository;

@Slf4j
@SuppressWarnings("checkstyle:MissingJavadocType")
public class ChangeStateDispatcherActor extends AbstractActor {

  public static String ACTOR_NAME = "change-state-dispatcher-actor";

  private final BlockingQueue<ResultCommandContainer> commandResultsQueue;
  private final StateRepository stateRepository;

  public ChangeStateDispatcherActor(
      StateRepository stateRepository, BlockingQueue<ResultCommandContainer> commandResultsQueue) {
    this.commandResultsQueue = commandResultsQueue;
    this.stateRepository = stateRepository;
  }

  public static Props props(
      StateRepository stateRepository, BlockingQueue<ResultCommandContainer> commandResultsQueue) {
    return Props.create(ChangeStateDispatcherActor.class, stateRepository, commandResultsQueue);
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(
            InputCommandMessage.class,
            message -> {
              if (message.getType() != null) {
                switch (message.getType()) {
                  case START_SERVE:
                    stateRepository.updateProcessingState(IsProcessedState.ENABLE);
                    commandResultsQueue.put(
                        ResultCommandContainer.builder()
                            .replyType(getReplyType(message))
                            .resultMessage(message.getPayload().getMessageValue())
                            .replyChatId(message.getPayload().getSenderChatId())
                            .platformType(message.getPlatformType())
                            .build());
                    break;
                  case STOP_SERVE:
                    stateRepository.updateProcessingState(IsProcessedState.DISABLE);
                    commandResultsQueue.put(
                        ResultCommandContainer.builder()
                            .replyType(getReplyType(message))
                            .resultMessage(message.getPayload().getMessageValue())
                            .replyChatId(message.getPayload().getSenderChatId())
                            .platformType(message.getPlatformType())
                            .build());
                    break;
                  default:
                    break;
                }
              }
            })
        .matchAny(o -> log.warn("received unknown message"))
        .build();
  }
}
