package org.yagi.motel.kernel.actor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import java.util.concurrent.BlockingQueue;
import lombok.extern.slf4j.Slf4j;
import org.yagi.motel.kernel.model.container.ResultCommandContainer;

@Slf4j
@SuppressWarnings("checkstyle:MissingJavadocType")
public class ErrorCommandDispatcherActor extends AbstractActor {

  public static String ACTOR_NAME = "error-command-dispatcher-actor";

  private final BlockingQueue<ResultCommandContainer> commandResultsQueue;

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public ErrorCommandDispatcherActor(BlockingQueue<ResultCommandContainer> commandResultsQueue) {
    this.commandResultsQueue = commandResultsQueue;
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static Props props(BlockingQueue<ResultCommandContainer> commandResultsQueue) {
    return Props.create(ErrorCommandDispatcherActor.class, commandResultsQueue);
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(
            ResultCommandContainer.class,
            commandResultsQueue::put)
        .matchAny(o -> log.warn("received unknown message"))
        .build();
  }
}
