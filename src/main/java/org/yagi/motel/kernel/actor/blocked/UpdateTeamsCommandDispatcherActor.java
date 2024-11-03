package org.yagi.motel.kernel.actor.blocked;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import lombok.extern.slf4j.Slf4j;
import org.yagi.motel.config.AppConfig;
import org.yagi.motel.http.RestClient;
import org.yagi.motel.http.request.SendTeamNamesToPantheonRequest;
import org.yagi.motel.http.response.SendTeamNamesToPantheonResponse;
import org.yagi.motel.kernel.message.InputCommandMessage;
import org.yagi.motel.kernel.model.container.ResultCommandContainer;
import org.yagi.motel.utils.UrlHelper;

@Slf4j
@SuppressWarnings("checkstyle:MissingJavadocType")
public class UpdateTeamsCommandDispatcherActor extends AbstractActor {

  public static String ACTOR_NAME = "update-teams-command-dispatcher-actor";

  private final BlockingQueue<ResultCommandContainer> commandResultsQueue;
  private final AppConfig config;
  private final ObjectMapper mapper;
  private final String portalSendTeamNamesToPantheonUrl;

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public UpdateTeamsCommandDispatcherActor(
      AppConfig config, BlockingQueue<ResultCommandContainer> commandResultsQueue) {
    this.config = config;
    this.commandResultsQueue = commandResultsQueue;
    this.mapper = new ObjectMapper();
    this.portalSendTeamNamesToPantheonUrl =
        UrlHelper.normalizeUrl(
            String.format("%s/api/v0/autobot/send_team_names_to_pantheon", config.getPortalUrl()));
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static Props props(
      AppConfig config, BlockingQueue<ResultCommandContainer> commandResultsQueue) {
    return Props.create(UpdateTeamsCommandDispatcherActor.class, config, commandResultsQueue);
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(
            InputCommandMessage.class,
            message -> {
              if (message.getType() != null) {
                switch (message.getType()) {
                  case UPDATE_TEAMS:
                    SendTeamNamesToPantheonRequest request =
                        SendTeamNamesToPantheonRequest.builder()
                            .apiToken(config.getAutobotApiToken())
                            .tournamentId(config.getTournamentId())
                            .lobbyId(config.getLobbyId())
                            .lang(message.getRequestedResponseLang())
                            .build();

                    Optional<SendTeamNamesToPantheonResponse> sendTeamNamesToPantheonResponse =
                        RestClient.sendPost(
                            mapper,
                            RestClient.preparePostRequest(
                                portalSendTeamNamesToPantheonUrl, request, mapper),
                            SendTeamNamesToPantheonResponse.class);

                    if (sendTeamNamesToPantheonResponse.isPresent()) {
                      commandResultsQueue.put(
                          ResultCommandContainer.builder()
                              .uniqueMessageId(message.getMessageUniqueId())
                              .resultMessage(sendTeamNamesToPantheonResponse.get().getMessage())
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
