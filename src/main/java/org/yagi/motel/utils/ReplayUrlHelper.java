package org.yagi.motel.utils;

import lombok.experimental.UtilityClass;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@UtilityClass
public class ReplayUrlHelper {

    private static final String PAIPU_KEY = "paipu";

    public static String extractHash(String url) {
        try {
            URL aURL = new URL(url);
            List<NameValuePair> params = URLEncodedUtils.parse(aURL.getQuery(), StandardCharsets.UTF_8);
            String hashString = params.stream()
                    .filter(pair -> PAIPU_KEY.equals(pair.getName()))
                    .findFirst()
                    .map(NameValuePair::getValue)
                    .orElse(null);
            return Optional.ofNullable(hashString)
                    .map(hash -> {
                        int playerPartIndex = hash.indexOf("_");
                        if (playerPartIndex != -1) {
                            return hash.substring(0, playerPartIndex);
                        } else {
                            return hash;
                        }
                    }).orElse(null);
        } catch (Exception ex) {
            //todo handle error
            return null;
        }
    }
}
