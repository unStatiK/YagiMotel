package org.yagi.motel.kernel.model.enums;

import java.util.Optional;
import lombok.Getter;

@Getter
@SuppressWarnings("checkstyle:MissingJavadocType")
public enum IsProcessedState {
  ENABLE(1),
  DISABLE(0);

  private final int state;

  IsProcessedState(int state) {
    this.state = state;
  }

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static Optional<IsProcessedState> resolveByValue(int value) {
    for (IsProcessedState state : values()) {
      if (state.state == value) {
        return Optional.of(state);
      }
    }
    return Optional.empty();
  }
}
