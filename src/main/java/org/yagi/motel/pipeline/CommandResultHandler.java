package org.yagi.motel.pipeline;

import java.util.concurrent.BlockingQueue;
import org.yagi.motel.model.container.CommunicationPlatformsContainer;
import org.yagi.motel.model.container.ResultCommandContainer;

@SuppressWarnings("checkstyle:MissingJavadocType")
public class CommandResultHandler implements Runnable {

  private final BlockingQueue<ResultCommandContainer> commandResultsQueue;
  private final CommunicationPlatformsContainer communicationPlatformsContainer;

  public CommandResultHandler(
      BlockingQueue<ResultCommandContainer> commandResultsQueue,
      CommunicationPlatformsContainer communicationPlatformsContainer) {
    this.commandResultsQueue = commandResultsQueue;
    this.communicationPlatformsContainer = communicationPlatformsContainer;
  }

  @Override
  public void run() {
    do {
      try {
        ResultCommandContainer resultCommandContainer = commandResultsQueue.take();
        switch (resultCommandContainer.getPlatformType()) {
          case TG:
            communicationPlatformsContainer.getTgBot().sendReply(resultCommandContainer);
            break;
          case DISCORD:
            communicationPlatformsContainer.getDiscordBot().sendReply(resultCommandContainer);
            break;
          default:
            break;
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    } while (!Thread.interrupted());
  }
}
