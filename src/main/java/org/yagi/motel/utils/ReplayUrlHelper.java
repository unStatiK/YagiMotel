package org.yagi.motel.utils;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

@UtilityClass
@SuppressWarnings("checkstyle:MissingJavadocType")
public class ReplayUrlHelper {

  private static final String PAIPU_KEY = "paipu";

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static Optional<String> extractHash(String url) {
    try {
      URL sourceUrl = new URL(url);
      List<NameValuePair> params =
          URLEncodedUtils.parse(sourceUrl.getQuery(), StandardCharsets.UTF_8);
      String hashString =
          params.stream()
              .filter(pair -> PAIPU_KEY.equals(pair.getName()))
              .findFirst()
              .map(NameValuePair::getValue)
              .orElse(null);
      return Optional.ofNullable(hashString)
          .map(
              hash -> {
                int playerPartIndex = hash.indexOf("_");
                if (playerPartIndex != -1) {
                  return hash.substring(0, playerPartIndex);
                } else {
                  return hash;
                }
              });
    } catch (Exception ex) {
      // todo handle error
      return Optional.empty();
    }
  }
}
