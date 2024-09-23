package org.yagi.motel.actor.blocked;

import static org.yagi.motel.utils.ReplyTypeUtils.getReplyType;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import lombok.extern.slf4j.Slf4j;
import org.yagi.motel.config.AppConfig;
import org.yagi.motel.http.RestClient;
import org.yagi.motel.message.InputCommandMessage;
import org.yagi.motel.model.container.ResultCommandContainer;
import org.yagi.motel.request.CloseRegistrationRequest;
import org.yagi.motel.response.BaseResponse;
import org.yagi.motel.utils.UrlHelper;

@Slf4j
@SuppressWarnings("checkstyle:MissingJavadocType")
public class CloseRegistrationCommandDispatcherActor extends AbstractActor {

  public static String ACTOR_NAME = "close-registration-command-dispatcher-actor";

  private final BlockingQueue<ResultCommandContainer> commandResultsQueue;
  private final AppConfig config;
  private final ObjectMapper mapper;
  private final String portalCloseRegistrationUrl;

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public CloseRegistrationCommandDispatcherActor(
      BlockingQueue<ResultCommandContainer> commandResultsQueue, AppConfig config) {
    this.commandResultsQueue = commandResultsQueue;
    this.config = config;
    this.mapper = new ObjectMapper();
    this.portalCloseRegistrationUrl =
        UrlHelper.normalizeUrl(
            String.format("%s/api/v0/autobot/close_registration", config.getPortalUrl()));
  }

  public static Props props(
      BlockingQueue<ResultCommandContainer> commandResultsQueue, AppConfig config) {
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
                    CloseRegistrationRequest request =
                        CloseRegistrationRequest.builder()
                            .apiToken(config.getAutobotApiToken())
                            .tournamentId(config.getTournamentId())
                            .lobbyId(config.getLobbyId())
                            .build();

                    Optional<BaseResponse> baseResponse =
                        RestClient.sendPost(
                            mapper,
                            RestClient.preparePostRequest(
                                portalCloseRegistrationUrl, request, mapper),
                            BaseResponse.class);

                    if (baseResponse.isPresent()
                        && Boolean.TRUE.equals(baseResponse.get().getSuccess())) {
                      commandResultsQueue.put(
                          ResultCommandContainer.builder()
                              .replyType(getReplyType(message))
                              .resultMessage(message.getPayload().getMessageValue())
                              .replyChatId(message.getPayload().getSenderChatId())
                              .platformType(message.getPlatformType())
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
