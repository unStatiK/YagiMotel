package org.yagi.motel.actor.blocked;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.yagi.motel.bot.ReplyType;
import org.yagi.motel.config.AppConfig;
import org.yagi.motel.http.RestClient;
import org.yagi.motel.message.InputCommandMessage;
import org.yagi.motel.model.container.ResultCommandContainer;
import org.yagi.motel.request.StatusRequest;
import org.yagi.motel.response.StatusResponse;
import org.yagi.motel.utils.UrlHelper;

import java.util.concurrent.BlockingQueue;

@Slf4j
public class StatusCommandDispatcherActor extends AbstractActor {

    public static String ACTOR_NAME = "status-command-dispatcher-actor";

    private final BlockingQueue<ResultCommandContainer> commandResultsQueue;
    private final AppConfig config;
    private final ObjectMapper mapper;
    private final String portalStatusUrl;

    public StatusCommandDispatcherActor(BlockingQueue<ResultCommandContainer> commandResultsQueue,
                                        AppConfig config) {
        this.commandResultsQueue = commandResultsQueue;
        this.config = config;
        this.mapper = new ObjectMapper();
        this.portalStatusUrl =
                UrlHelper.normalizeUrl(String.format("%s/api/v0/autobot/get_tournament_status", config.getPortalUrl()));
    }

    public static Props props(BlockingQueue<ResultCommandContainer> commandResultsQueue,
                              AppConfig config) {
        return Props.create(StatusCommandDispatcherActor.class, commandResultsQueue, config);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(
                        InputCommandMessage.class,
                        message -> {
                            if (message.getType() != null) {
                                switch (message.getType()) {
                                    case STATUS:
                                        StatusRequest request = StatusRequest.builder()
                                                .apiToken(config.getAutobotApiToken())
                                                .tournamentId(config.getTournamentId())
                                                .lobbyId(config.getLobbyId())
                                                .build();

                                        StatusResponse statusResponse =
                                                RestClient.sendPost(mapper,
                                                        RestClient.preparePostRequest(portalStatusUrl, request, mapper),
                                                        StatusResponse.class);

                                        if (statusResponse != null) {
                                            commandResultsQueue.put(ResultCommandContainer.builder()
                                                    .replyType(ReplyType.SEND_MESSAGE)
                                                    .resultMessage(statusResponse.getMessage())
                                                    .replyChatId(message.getPayload().getSenderChatId())
                                                    .build());
                                        }
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
