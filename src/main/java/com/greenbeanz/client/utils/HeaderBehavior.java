package com.greenbeanz.client.utils;

import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;

/** Controls how the shared SMLS menu header uses vertical space. */
public enum HeaderBehavior {
    ALWAYS_BIG("SMLS.option.header_behavior.always_big"),
    ALWAYS_SMALL("SMLS.option.header_behavior.always_small"),
    ON_SCROLL("SMLS.option.header_behavior.on_scroll");

    private final String translationKey;

    HeaderBehavior(String translationKey) {
        this.translationKey = translationKey;
    }

    public Component displayName() {
        return Component.translatable(translationKey);
    }

    public static List<HeaderBehavior> options() {
        return List.of(values());
    }

    public static HeaderBehavior parse(String value) {
        if (value == null) return ON_SCROLL;
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return ON_SCROLL;
        }
    }
}
