package org.yagi.motel.http.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@SuppressWarnings("checkstyle:MissingJavadocType")
public class AddGameLogResponse {
  private String message;

  @JsonProperty("is_error")
  private Boolean isError;
}
