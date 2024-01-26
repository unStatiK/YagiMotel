package org.yagi.motel.pipeline;

import org.yagi.motel.bot.TournamentHelper;
import org.yagi.motel.model.container.ResultCommandContainer;

import java.util.concurrent.BlockingQueue;

public class CommandResultHandler implements Runnable {

    private final BlockingQueue<ResultCommandContainer> commandResultsQueue;
    private final TournamentHelper bot;

    public CommandResultHandler(BlockingQueue<ResultCommandContainer> commandResultsQueue,
                                TournamentHelper bot) {
        this.commandResultsQueue = commandResultsQueue;
        this.bot = bot;
    }

    @Override
    public void run() {
        do {
            try {
                ResultCommandContainer resultCommandContainer = commandResultsQueue.take();
                bot.sendReply(resultCommandContainer);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } while (!Thread.interrupted());
    }
}
