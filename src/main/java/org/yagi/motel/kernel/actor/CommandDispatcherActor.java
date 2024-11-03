package org.yagi.motel.kernel.actor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import lombok.extern.slf4j.Slf4j;
import org.yagi.motel.kernel.message.InputCommandMessage;
import org.yagi.motel.kernel.model.holder.CommandDispatchersHolder;

@Slf4j
@SuppressWarnings("checkstyle:MissingJavadocType")
public class CommandDispatcherActor extends AbstractActor {

  public static String ACTOR_NAME = "command-dispatcher-actor";

  private final CommandDispatchersHolder commandDispatchersHolder;

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public CommandDispatcherActor(CommandDispatchersHolder commandDispatchersHolder) {
    this.commandDispatchersHolder = commandDispatchersHolder;
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static Props props(CommandDispatchersHolder commandDispatchersHolder) {
    return Props.create(CommandDispatcherActor.class, commandDispatchersHolder);
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
                    commandDispatchersHolder
                        .getLogCommandDispatcherActor()
                        .tell(message, getSelf());
                    break;
                  case ME:
                    commandDispatchersHolder.getMeCommandDispatcherActor().tell(message, getSelf());
                    break;
                  case ADD:
                    commandDispatchersHolder
                        .getAddCommandDispatcherActor()
                        .tell(message, getSelf());
                    break;
                  case START_SERVE:
                  case STOP_SERVE:
                    commandDispatchersHolder
                        .getChangeStateDispatcherActor()
                        .tell(message, getSelf());
                    break;
                  case START_REGISTRATION:
                    commandDispatchersHolder
                        .getStartRegistrationCommandDispatcherActor()
                        .tell(message, getSelf());
                    break;
                  case CLOSE_REGISTRATION:
                    commandDispatchersHolder
                        .getCloseRegistrationCommandDispatcherActor()
                        .tell(message, getSelf());
                    break;
                  case STATUS:
                    commandDispatchersHolder
                        .getStatusCommandDispatcherActor()
                        .tell(message, getSelf());
                    break;
                  case UPDATE_TEAMS:
                    commandDispatchersHolder
                        .getUpdateTeamsDispatcherActor()
                        .tell(message, getSelf());
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
