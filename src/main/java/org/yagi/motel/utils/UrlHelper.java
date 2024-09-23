package org.yagi.motel.utils;

import java.net.URI;
import java.net.URISyntaxException;
import lombok.experimental.UtilityClass;

@UtilityClass
@SuppressWarnings("checkstyle:MissingJavadocType")
public class UrlHelper {
  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static String normalizeUrl(String url) {
    try {
      return new URI(url).normalize().toString();
    } catch (URISyntaxException e) {
      return url;
    }
  }
}
