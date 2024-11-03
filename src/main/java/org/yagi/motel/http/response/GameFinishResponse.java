package org.yagi.motel.http.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@SuppressWarnings("checkstyle:MissingJavadocType")
public class GameFinishResponse {
  private String message;

  @JsonProperty("is_error")
  private Boolean isError;
}
