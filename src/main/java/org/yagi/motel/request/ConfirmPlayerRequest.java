package org.yagi.motel.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConfirmPlayerRequest {
    @JsonProperty("api_token")
    private String apiToken;
    @JsonProperty("tournament_id")
    private Long tournamentId;
    @JsonProperty("lobby_id")
    private Long lobbyId;
    private String nickname;
    @JsonProperty("telegram_username")
    private String telegramUsername;
}
