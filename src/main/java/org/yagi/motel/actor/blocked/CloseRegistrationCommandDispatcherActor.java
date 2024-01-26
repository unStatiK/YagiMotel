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
import org.yagi.motel.request.CloseRegistrationRequest;
import org.yagi.motel.response.BaseResponse;
import org.yagi.motel.utils.UrlHelper;

import java.util.concurrent.BlockingQueue;

@Slf4j
public class CloseRegistrationCommandDispatcherActor extends AbstractActor {

    public static String ACTOR_NAME = "close-registration-command-dispatcher-actor";

    private final BlockingQueue<ResultCommandContainer> commandResultsQueue;
    private final AppConfig config;
    private final ObjectMapper mapper;
    private final String portalCloseRegistrationUrl;

    public CloseRegistrationCommandDispatcherActor(BlockingQueue<ResultCommandContainer> commandResultsQueue,
                                                   AppConfig config) {
        this.commandResultsQueue = commandResultsQueue;
        this.config = config;
        this.mapper = new ObjectMapper();
        this.portalCloseRegistrationUrl =
                UrlHelper.normalizeUrl(String.format("%s/api/v0/autobot/close_registration", config.getPortalUrl()));
    }

    public static Props props(BlockingQueue<ResultCommandContainer> commandResultsQueue,
                              AppConfig config) {
        return Props.create(CloseRegistrationCommandDispatcherActor.class, commandResultsQueue, config);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(
                        InputCommandMessage.class,
                        message -> {
                            if (message.getType() != null) {
                                switch (message.getType()) {
                                    case CLOSE_REGISTRATION:
                                        CloseRegistrationRequest request = CloseRegistrationRequest.builder()
                                                .apiToken(config.getAutobotApiToken())
                                                .tournamentId(config.getTournamentId())
                                                .lobbyId(config.getLobbyId())
                                                .build();

                                        BaseResponse baseResponse =
                                                RestClient.sendPost(mapper,
                                                        RestClient.preparePostRequest(portalCloseRegistrationUrl, request, mapper),
                                                        BaseResponse.class);

                                        if (baseResponse != null && Boolean.TRUE.equals(baseResponse.getSuccess())) {
                                            commandResultsQueue.put(ResultCommandContainer.builder()
                                                    .replyType(ReplyType.SEND_MESSAGE)
                                                    .resultMessage(message.getPayload().getMessageValue())
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
