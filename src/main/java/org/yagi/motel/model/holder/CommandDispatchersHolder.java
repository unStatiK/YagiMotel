package org.yagi.motel.model.holder;

import akka.actor.ActorRef;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommandDispatchersHolder {
    private ActorRef logCommandDispatcherActor;
    private ActorRef startRegistrationCommandDispatcherActor;
    private ActorRef closeRegistrationCommandDispatcherActor;
    private ActorRef meCommandDispatcherActor;
    private ActorRef addCommandDispatcherActor;
    private ActorRef statusCommandDispatcherActor;
}
