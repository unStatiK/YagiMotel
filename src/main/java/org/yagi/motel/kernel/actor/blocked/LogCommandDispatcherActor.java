package org.yagi.motel.kernel.actor.blocked;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.yagi.motel.config.AppConfig;
import org.yagi.motel.http.RestClient;
import org.yagi.motel.http.request.AddGameLogRequest;
import org.yagi.motel.http.request.GameFinishRequest;
import org.yagi.motel.http.response.AddGameLogResponse;
import org.yagi.motel.http.response.GameFinishResponse;
import org.yagi.motel.kernel.message.InputCommandMessage;
import org.yagi.motel.kernel.model.container.ResultCommandContainer;
import org.yagi.motel.utils.ReplayUrlHelper;
import org.yagi.motel.utils.UrlHelper;

@Slf4j
@SuppressWarnings("checkstyle:MissingJavadocType")
public class LogCommandDispatcherActor extends AbstractActor {

  public static String ACTOR_NAME = "log-command-dispatcher-actor";

  private final AppConfig config;
  private final BlockingQueue<ResultCommandContainer> commandResultsQueue;
  private final ObjectMapper mapper;
  private final String portalGameFinishUrl;
  private final String portalAddGameLogUrl;

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public LogCommandDispatcherActor(
      AppConfig config, BlockingQueue<ResultCommandContainer> commandResultsQueue) {
    this.config = config;
    this.commandResultsQueue = commandResultsQueue;
    this.mapper = getTensoulMapper();
    this.portalGameFinishUrl =
        UrlHelper.normalizeUrl(
            String.format("%s/api/v0/autobot/game_finish", config.getPortalUrl()));
    this.portalAddGameLogUrl =
        UrlHelper.normalizeUrl(
            String.format("%s/api/v0/autobot/add_game_log", config.getPortalUrl()));
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static ObjectMapper getTensoulMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.USE_LONG_FOR_INTS, true);
    return objectMapper;
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static Props props(
      AppConfig config, BlockingQueue<ResultCommandContainer> commandResultsQueue) {
    return Props.create(LogCommandDispatcherActor.class, config, commandResultsQueue);
  }

  private void processMajsoulLog(InputCommandMessage message)
      throws IOException, InterruptedException {
    Optional<String> hashOptional =
        ReplayUrlHelper.extractHash(message.getPayload().getMessageValue());
    if (hashOptional.isPresent() && !StringUtils.isEmpty(hashOptional.get())) {
      String hash = hashOptional.get();
      String tensoulUrl =
          String.format(
              "%s/convert/?id=%s&lobby_id=%d&app_token=%s",
              config.getTensoulUrl(), hash, config.getLobbyId(), config.getTensoulAppToken());
      tensoulUrl = UrlHelper.normalizeUrl(tensoulUrl);
      Optional<Map> replayInfo =
          RestClient.sendGet(mapper, RestClient.prepareGetRequest(tensoulUrl), Map.class);

      if (replayInfo.isPresent()) {
        Optional<GameFinishRequest> gameFinishRequest =
            GameFinishRequest.convertFromTensoulMap(config, mapper, replayInfo.get());
        if (gameFinishRequest.isPresent()) {
          // todo handle error
          Optional<GameFinishResponse> response =
              RestClient.sendPost(
                  mapper,
                  RestClient.preparePostRequest(
                      portalGameFinishUrl, gameFinishRequest.get(), mapper),
                  GameFinishResponse.class);

          if (response.isPresent()) {
            commandResultsQueue.put(
                ResultCommandContainer.builder()
                    .uniqueMessageId(message.getMessageUniqueId())
                    .resultMessage(response.get().getMessage())
                    .replyChatId(message.getPayload().getSenderChatId())
                    .platformType(message.getPlatformType())
                    .build());
          } else {
            commandResultsQueue.put(
                ResultCommandContainer.builder()
                    .uniqueMessageId(message.getMessageUniqueId())
                    .resultMessage("Ошибка добавления реплея на портал!")
                    .replyChatId(message.getPayload().getSenderChatId())
                    .platformType(message.getPlatformType())
                    .build());
          }

        } else {
          commandResultsQueue.put(
              ResultCommandContainer.builder()
                  .uniqueMessageId(message.getMessageUniqueId())
                  .resultMessage(
                      String.format("Получен невалидный tensoul контент для реплея %s!", hash))
                  .replyChatId(message.getPayload().getSenderChatId())
                  .platformType(message.getPlatformType())
                  .build());
        }

      } else {
        commandResultsQueue.put(
            ResultCommandContainer.builder()
                .uniqueMessageId(message.getMessageUniqueId())
                .resultMessage("Не удалось сконвертировать реплей в tenhou.net формат!")
                .replyChatId(message.getPayload().getSenderChatId())
                .platformType(message.getPlatformType())
                .build());
      }
    } else {
      commandResultsQueue.put(
          ResultCommandContainer.builder()
              .uniqueMessageId(message.getMessageUniqueId())
              .resultMessage("Не удалось извлечь id реплея, проверьте URL!")
              .replyChatId(message.getPayload().getSenderChatId())
              .platformType(message.getPlatformType())
              .build());
    }
  }

  private void processTenhouLog(InputCommandMessage message)
      throws IOException, InterruptedException {
    String tenhouLogLink = message.getPayload().getMessageValue();
    AddGameLogRequest addGameLogRequest =
        AddGameLogRequest.builder()
            .apiToken(config.getAutobotApiToken())
            .tournamentId(config.getTournamentId())
            .lobbyId(config.getLobbyId())
            .logLink(tenhouLogLink)
            .build();

    Optional<AddGameLogResponse> addGameLogResponse =
        RestClient.sendPost(
            mapper,
            RestClient.preparePostRequest(portalAddGameLogUrl, addGameLogRequest, mapper),
            AddGameLogResponse.class);

    if (addGameLogResponse.isPresent()) {
      commandResultsQueue.put(
          ResultCommandContainer.builder()
              .uniqueMessageId(message.getMessageUniqueId())
              .resultMessage(addGameLogResponse.get().getMessage())
              .replyChatId(message.getPayload().getSenderChatId())
              .platformType(message.getPlatformType())
              .build());
    } else {
      commandResultsQueue.put(
          ResultCommandContainer.builder()
              .uniqueMessageId(message.getMessageUniqueId())
              .resultMessage("Ошибка добавления реплея на портал!")
              .replyChatId(message.getPayload().getSenderChatId())
              .platformType(message.getPlatformType())
              .build());
    }
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(
            InputCommandMessage.class,
            message -> {
              if (message.getType() != null) {
                switch (message.getType()) {
                  case LOG:
                    if (message.getPayload().getMessageValue().contains("tenhou.net")) {
                      processTenhouLog(message);
                    } else {
                      processMajsoulLog(message);
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
