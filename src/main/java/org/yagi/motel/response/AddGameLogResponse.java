package org.yagi.motel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AddGameLogResponse {
    private String message;
    @JsonProperty("is_error")
    private Boolean isError;
}
