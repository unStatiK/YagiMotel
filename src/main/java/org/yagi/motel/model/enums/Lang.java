package org.yagi.motel.model.enums;

import lombok.Getter;

@Getter
@SuppressWarnings("checkstyle:MissingJavadocType")
public enum Lang {
  RU("ru"), EN("en");

  private final String lang;

  Lang(String lang) {
    this.lang = lang;
  }
}
