package org.yagi.motel.actor.blocked;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.yagi.motel.bot.ReplyType;
import org.yagi.motel.config.AppConfig;
import org.yagi.motel.http.RestClient;
import org.yagi.motel.message.InputCommandMessage;
import org.yagi.motel.model.container.ResultCommandContainer;
import org.yagi.motel.request.AddGameLogRequest;
import org.yagi.motel.request.GameFinishRequest;
import org.yagi.motel.response.AddGameLogResponse;
import org.yagi.motel.response.GameFinishResponse;
import org.yagi.motel.utils.ReplayUrlHelper;
import org.yagi.motel.utils.UrlHelper;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

@Slf4j
public class LogCommandDispatcherActor extends AbstractActor {

    public static String ACTOR_NAME = "log-command-dispatcher-actor";

    private final AppConfig config;
    private final BlockingQueue<ResultCommandContainer> commandResultsQueue;
    private final ObjectMapper mapper;
    private final String portalGameFinishUrl;
    private final String portalAddGameLogUrl;

    public LogCommandDispatcherActor(AppConfig config, BlockingQueue<ResultCommandContainer> commandResultsQueue) {
        this.config = config;
        this.commandResultsQueue = commandResultsQueue;
        this.mapper = getTensoulMapper();
        this.portalGameFinishUrl =
                UrlHelper.normalizeUrl(String.format("%s/api/v0/autobot/game_finish", config.getPortalUrl()));
        this.portalAddGameLogUrl =
                UrlHelper.normalizeUrl(String.format("%s/api/v0/autobot/add_game_log", config.getPortalUrl()));
    }

    public static ObjectMapper getTensoulMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.USE_LONG_FOR_INTS, true);
        return objectMapper;
    }

    public static Props props(AppConfig config, BlockingQueue<ResultCommandContainer> commandResultsQueue) {
        return Props.create(LogCommandDispatcherActor.class, config, commandResultsQueue);
    }

    private void processMajsoulLog(InputCommandMessage message) throws IOException, InterruptedException {
        String hash = ReplayUrlHelper.extractHash(message.getPayload().getMessageValue());
        if (!StringUtils.isEmpty(hash)) {
            String tensoulUrl = String.format("%s/convert/?id=%s&lobby_id=%d&app_token=%s", config.getTensoulUrl(),
                    hash, config.getLobbyId(), config.getTensoulAppToken());
            tensoulUrl = UrlHelper.normalizeUrl(tensoulUrl);
            Map replayInfo = RestClient.sendGet(mapper, RestClient.prepareGetRequest(tensoulUrl), Map.class);

            if (replayInfo != null) {
                GameFinishRequest gameFinishRequest =
                        GameFinishRequest.convertFromTensoulMap(config, mapper, replayInfo);
                if (gameFinishRequest != null) {
                    //todo handle error
                    GameFinishResponse response = RestClient.sendPost(mapper,
                            RestClient.preparePostRequest(portalGameFinishUrl, gameFinishRequest, mapper),
                            GameFinishResponse.class);

                    if (response != null) {
                        commandResultsQueue.put(ResultCommandContainer.builder()
                                .replyType(ReplyType.SEND_MESSAGE)
                                .resultMessage(response.getMessage())
                                .replyChatId(message.getPayload().getSenderChatId())
                                .build());
                    } else {
                        commandResultsQueue.put(ResultCommandContainer.builder()
                                .replyType(ReplyType.SEND_MESSAGE)
                                .resultMessage("Ошибка добавления реплея на портал!")
                                .replyChatId(message.getPayload().getSenderChatId())
                                .build());
                    }

                } else {
                    commandResultsQueue.put(ResultCommandContainer.builder()
                            .replyType(ReplyType.SEND_MESSAGE)
                            .resultMessage(String.format("Получен невалидный tensoul контент для реплея %s!", hash))
                            .replyChatId(message.getPayload().getSenderChatId())
                            .build());
                }

            } else {
                commandResultsQueue.put(ResultCommandContainer.builder()
                        .replyType(ReplyType.SEND_MESSAGE)
                        .resultMessage("Не удалось сконвертировать реплей в tenhou.net формат!")
                        .replyChatId(message.getPayload().getSenderChatId())
                        .build());
            }
        } else {
            commandResultsQueue.put(ResultCommandContainer.builder()
                    .replyType(ReplyType.SEND_MESSAGE)
                    .resultMessage("Не удалось извлечь id реплея, проверьте URL!")
                    .replyChatId(message.getPayload().getSenderChatId())
                    .build());
        }
    }

    private void processTenhouLog(InputCommandMessage message) throws IOException, InterruptedException {
        String tenhouLogLink = message.getPayload().getMessageValue();
        AddGameLogRequest addGameLogRequest = AddGameLogRequest.builder()
                .apiToken(config.getAutobotApiToken())
                .tournamentId(config.getTournamentId())
                .lobbyId(config.getLobbyId())
                .logLink(tenhouLogLink)
                .build();

        AddGameLogResponse addGameLogResponse = RestClient.sendPost(mapper,
                RestClient.preparePostRequest(portalAddGameLogUrl, addGameLogRequest, mapper),
                AddGameLogResponse.class);

        if (addGameLogResponse!= null) {
            commandResultsQueue.put(ResultCommandContainer.builder()
                    .replyType(ReplyType.SEND_MESSAGE)
                    .resultMessage(addGameLogResponse.getMessage())
                    .replyChatId(message.getPayload().getSenderChatId())
                    .build());
        } else {
            commandResultsQueue.put(ResultCommandContainer.builder()
                    .replyType(ReplyType.SEND_MESSAGE)
                    .resultMessage("Ошибка добавления реплея на портал!")
                    .replyChatId(message.getPayload().getSenderChatId())
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
