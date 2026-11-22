package com.greenbeanz.client.modules;

import net.minecraft.network.chat.Component;

/** Staff departments whose internal rank is configured independently. */
public enum StaffDepartment {
    BANS("SMLS.department.bans"),
    EVENTS("SMLS.department.events"),
    WAR("SMLS.department.war");

    private final String translationKey;

    StaffDepartment(String translationKey) {
        this.translationKey = translationKey;
    }

    public Component displayName() {
        return Component.translatable(translationKey);
    }
}
