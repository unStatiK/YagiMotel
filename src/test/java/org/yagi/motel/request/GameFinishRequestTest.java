package org.yagi.motel.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.yagi.motel.config.AppConfig;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import org.yagi.motel.http.request.GameFinishRequest;
import org.yagi.motel.kernel.actor.blocked.LogCommandDispatcherActor;

public class GameFinishRequestTest {

    private final ObjectMapper objectMapper = LogCommandDispatcherActor.getTensoulMapper();

    @Test
    public void testBuildGameFinishRequest() throws IOException {
        String autobotToken = "token";
        Long lobbyId = 1L;
        Long tournamentId = 2L;

        ClassLoader classLoader = getClass().getClassLoader();
        Map tensoulReplayInfo = objectMapper.readValue(
                new File(classLoader.getResource("tensoul.json").getFile()), Map.class);
        Map replayMap = (Map) tensoulReplayInfo.get("log");

        AppConfig config = new AppConfig();
        config.setAutobotApiToken(autobotToken);
        config.setLobbyId(lobbyId);
        config.setTournamentId(tournamentId);

        GameFinishRequest gameFinishRequest =
                GameFinishRequest.convertFromTensoulMap(config, objectMapper, tensoulReplayInfo).get();

        Assert.assertNotNull(gameFinishRequest);
        Assert.assertEquals(autobotToken, gameFinishRequest.getApiToken());
        Assert.assertEquals(lobbyId, gameFinishRequest.getLobbyId());
        Assert.assertEquals(tournamentId, gameFinishRequest.getTournamentId());
        Assert.assertFalse(gameFinishRequest.getIsError());
        Assert.assertEquals("240120-dcc2fb60-e6fb-4d7b-ba8a-543a989fb2b4", gameFinishRequest.getLogId());
        Assert.assertEquals(4, gameFinishRequest.getPlayers().size());
        Assert.assertEquals(Arrays.asList("player1", "player2", "player3", "player4"), gameFinishRequest.getPlayers());
        Assert.assertEquals(objectMapper.readTree(objectMapper.writeValueAsString(replayMap)),
                objectMapper.readTree(gameFinishRequest.getLogContent()));
        Assert.assertEquals(1705688368L, (long) gameFinishRequest.getLogTime());
    }

    @Test
    public void testBuildGameFinishRequestWithSameNicknames() throws IOException {
        String autobotToken = "token";
        Long lobbyId = 1L;
        Long tournamentId = 2L;

        ClassLoader classLoader = getClass().getClassLoader();

        // https://game.mahjongsoul.com/?paipu=240121-dfbb61ab-9569-4c00-bc9f-6b878b2dbd6c
        // Japan account FakeToad with account_id = 101404344 should be at firstPlace with 34900 points
        Map tensoulReplayInfo = objectMapper.readValue(
                new File(classLoader.getResource("tensoul_with_same_nickname.json").getFile()), Map.class);
        Map replayMap = (Map) tensoulReplayInfo.get("log");

        AppConfig config = new AppConfig();
        config.setAutobotApiToken(autobotToken);
        config.setLobbyId(lobbyId);
        config.setTournamentId(tournamentId);

        GameFinishRequest gameFinishRequest =
                GameFinishRequest.convertFromTensoulMap(config, objectMapper, tensoulReplayInfo).get();

        Assert.assertNotNull(gameFinishRequest);
        Assert.assertEquals(autobotToken, gameFinishRequest.getApiToken());
        Assert.assertEquals(lobbyId, gameFinishRequest.getLobbyId());
        Assert.assertEquals(tournamentId, gameFinishRequest.getTournamentId());
        Assert.assertFalse(gameFinishRequest.getIsError());
        Assert.assertEquals("240121-dfbb61ab-9569-4c00-bc9f-6b878b2dbd6c", gameFinishRequest.getLogId());
        Assert.assertEquals(4, gameFinishRequest.getPlayers().size());
        Assert.assertEquals(Arrays.asList("FakeToad", "AI1", "AI2", "FakeToad"), gameFinishRequest.getPlayers());
        Assert.assertEquals(objectMapper.readTree(objectMapper.writeValueAsString(replayMap)),
                objectMapper.readTree(gameFinishRequest.getLogContent()));
        Assert.assertEquals(1705835593, (long) gameFinishRequest.getLogTime());
    }
}
