package org.yagi.motel.model.enums;

import java.util.Locale;

public enum GamePlatformType {
    TENHOU, MAJSOUL;

    public static GamePlatformType fromString(String platform) {
        try {
            return valueOf(platform.toUpperCase(Locale.US));
        } catch (Exception ex) {
            return TENHOU;
        }
    }
}
