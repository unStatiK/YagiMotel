package org.yagi.motel.kernel.actor.blocked;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import lombok.extern.slf4j.Slf4j;
import org.yagi.motel.config.AppConfig;
import org.yagi.motel.http.RestClient;
import org.yagi.motel.http.request.ConfirmPlayerRequest;
import org.yagi.motel.http.response.ConfirmPlayerResponse;
import org.yagi.motel.kernel.enums.PlatformType;
import org.yagi.motel.kernel.message.InputCommandMessage;
import org.yagi.motel.kernel.model.container.ResultCommandContainer;
import org.yagi.motel.utils.UrlHelper;

@Slf4j
@SuppressWarnings("checkstyle:MissingJavadocType")
public class MeCommandDispatcherActor extends AbstractActor {

  public static String ACTOR_NAME = "me-command-dispatcher-actor";

  private final BlockingQueue<ResultCommandContainer> commandResultsQueue;
  private final AppConfig config;
  private final ObjectMapper mapper;
  private final String portalConfirmPlayerUrl;

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public MeCommandDispatcherActor(
      AppConfig config, BlockingQueue<ResultCommandContainer> commandResultsQueue) {
    this.config = config;
    this.commandResultsQueue = commandResultsQueue;
    this.mapper = new ObjectMapper();
    this.portalConfirmPlayerUrl =
        UrlHelper.normalizeUrl(
            String.format("%s/api/v0/autobot/confirm_player", config.getPortalUrl()));
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static Props props(
      AppConfig config, BlockingQueue<ResultCommandContainer> commandResultsQueue) {
    return Props.create(MeCommandDispatcherActor.class, config, commandResultsQueue);
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(
            InputCommandMessage.class,
            message -> {
              if (message.getType() != null) {
                switch (message.getType()) {
                  case ME:
                    ConfirmPlayerRequest request =
                        ConfirmPlayerRequest.builder()
                            .apiToken(config.getAutobotApiToken())
                            .tournamentId(config.getTournamentId())
                            .lobbyId(config.getLobbyId())
                            .nickname(message.getPayload().getMessageValue())
                            .telegramUsername(
                                message.getPlatformType() == PlatformType.TG
                                    ? message.getPayload().getUsername()
                                    : null)
                            .discordUsername(
                                message.getPlatformType() == PlatformType.DISCORD
                                    ? message.getPayload().getUsername()
                                    : null)
                            .lang(message.getRequestedResponseLang())
                            .build();

                    Optional<ConfirmPlayerResponse> confirmPlayerResponse =
                        RestClient.sendPost(
                            mapper,
                            RestClient.preparePostRequest(portalConfirmPlayerUrl, request, mapper),
                            ConfirmPlayerResponse.class);

                    if (confirmPlayerResponse.isPresent()) {
                      commandResultsQueue.put(
                          ResultCommandContainer.builder()
                              .uniqueMessageId(message.getMessageUniqueId())
                              .resultMessage(
                                  String.format(
                                      message.getPlatformType() == PlatformType.TG
                                          ? "@%s %s"
                                          : "%s %s",
                                      message.getPayload().getUsername(),
                                      confirmPlayerResponse.get().getMessage()))
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
