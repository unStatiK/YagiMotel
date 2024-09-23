package org.yagi.motel.mailbox;

import akka.actor.ActorSystem;
import akka.dispatch.PriorityGenerator;
import akka.dispatch.UnboundedPriorityMailbox;
import com.typesafe.config.Config;
import org.yagi.motel.message.InputCommandMessage;

@SuppressWarnings("checkstyle:MissingJavadocType")
public class PriorityMailbox extends UnboundedPriorityMailbox {

  // Warning! Should be equals in application.conf
  public static final String DISPATCHER_NAME = "priority-dispatcher";
  private static final int HIGH_PRIORITY = 1;
  private static final int LOW_PRIORITY = 0;

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public PriorityMailbox(ActorSystem.Settings settings, Config config) {
    super(
        new PriorityGenerator() {
          @Override
          public int gen(Object message) {
            if (message instanceof InputCommandMessage) {
              return HIGH_PRIORITY;
            }
            return LOW_PRIORITY;
          }
        });
  }
}
