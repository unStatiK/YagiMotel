package org.yagi.motel.utils;

import lombok.experimental.UtilityClass;

import java.net.URI;
import java.net.URISyntaxException;

@UtilityClass
public class UrlHelper {
    public static String normalizeUrl(String url) {
        try {
            return new URI(url).normalize().toString();
        } catch (URISyntaxException e) {
            return url;
        }
    }
}
