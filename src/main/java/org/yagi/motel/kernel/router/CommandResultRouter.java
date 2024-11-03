package org.yagi.motel.kernel.router;

import java.util.concurrent.BlockingQueue;
import org.yagi.motel.kernel.model.container.ResultCommandContainer;

@SuppressWarnings("checkstyle:MissingJavadocType")
public class CommandResultRouter implements Runnable {

  private final BlockingQueue<ResultCommandContainer> commandResultsQueue;
  private final BlockingQueue<ResultCommandContainer> tgMessagesQueue;
  private final BlockingQueue<ResultCommandContainer> discordMessagesQueue;

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public CommandResultRouter(
      BlockingQueue<ResultCommandContainer> commandResultsQueue,
      BlockingQueue<ResultCommandContainer> tgMessagesQueue,
      BlockingQueue<ResultCommandContainer> discordMessagesQueue) {
    this.commandResultsQueue = commandResultsQueue;
    this.tgMessagesQueue = tgMessagesQueue;
    this.discordMessagesQueue = discordMessagesQueue;
  }

  @Override
  public void run() {
    do {
      try {
        ResultCommandContainer resultCommandContainer = commandResultsQueue.take();
        switch (resultCommandContainer.getPlatformType()) {
          case TG:
            tgMessagesQueue.put(resultCommandContainer);
            break;
          case DISCORD:
            discordMessagesQueue.put(resultCommandContainer);
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
