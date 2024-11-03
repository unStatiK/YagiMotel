package org.yagi.motel.kernel.actor.blocked;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import lombok.extern.slf4j.Slf4j;
import org.yagi.motel.config.AppConfig;
import org.yagi.motel.handler.AddCommandHandler;
import org.yagi.motel.http.RestClient;
import org.yagi.motel.http.request.AdminConfirmPlayerRequest;
import org.yagi.motel.http.response.ConfirmPlayerResponse;
import org.yagi.motel.kernel.message.InputCommandMessage;
import org.yagi.motel.kernel.model.container.ResultCommandContainer;
import org.yagi.motel.utils.UrlHelper;

@Slf4j
@SuppressWarnings("checkstyle:MissingJavadocType")
public class AddCommandDispatcherActor extends AbstractActor {

  public static String ACTOR_NAME = "add-command-dispatcher-actor";

  private final BlockingQueue<ResultCommandContainer> commandResultsQueue;
  private final AppConfig config;
  private final ObjectMapper mapper;
  private final String portalAdminConfirmPlayerUrl;

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public AddCommandDispatcherActor(
      AppConfig config, BlockingQueue<ResultCommandContainer> commandResultsQueue) {
    this.config = config;
    this.commandResultsQueue = commandResultsQueue;
    this.mapper = new ObjectMapper();
    this.portalAdminConfirmPlayerUrl =
        UrlHelper.normalizeUrl(
            String.format("%s/api/v0/autobot/admin_confirm_player", config.getPortalUrl()));
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static Props props(
      AppConfig config, BlockingQueue<ResultCommandContainer> commandResultsQueue) {
    return Props.create(AddCommandDispatcherActor.class, config, commandResultsQueue);
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(
            InputCommandMessage.class,
            message -> {
              if (message.getType() != null) {
                switch (message.getType()) {
                  case ADD:
                    Map commandContext = message.getPayload().getContext();
                    String msNickname =
                        (String) commandContext.get(AddCommandHandler.MS_NICKNAME_CONTEXT_KEY);
                    Long msFriendId =
                        (Long) commandContext.get(AddCommandHandler.MS_FRIEND_ID_CONTEXT_KEY);

                    AdminConfirmPlayerRequest request =
                        AdminConfirmPlayerRequest.builder()
                            .apiToken(config.getAutobotApiToken())
                            .tournamentId(config.getTournamentId())
                            .lobbyId(config.getLobbyId())
                            .nickname(msNickname)
                            .friendId(msFriendId)
                            .telegramUsername(message.getPayload().getUsername())
                            .build();

                    Optional<ConfirmPlayerResponse> confirmPlayerResponse =
                        RestClient.sendPost(
                            mapper,
                            RestClient.preparePostRequest(
                                portalAdminConfirmPlayerUrl, request, mapper),
                            ConfirmPlayerResponse.class);

                    if (confirmPlayerResponse.isPresent()) {
                      commandResultsQueue.put(
                          ResultCommandContainer.builder()
                              .uniqueMessageId(message.getMessageUniqueId())
                              .resultMessage(
                                  String.format(
                                      "@%s %s",
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
