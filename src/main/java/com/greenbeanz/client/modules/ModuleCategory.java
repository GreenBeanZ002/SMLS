package com.greenbeanz.client.modules;

import net.minecraft.network.chat.Component;

/** Stable display groups for modules on the SMLS home screen. */
public enum ModuleCategory {
    GENERAL("SMLS.module_category.general"),
    EVENTS("SMLS.module_category.events"),
    WAR("SMLS.module_category.war"),
    BANS("SMLS.module_category.bans"),
    SERVER_MANAGEMENT("SMLS.module_category.server_management"),
    JOKE("SMLS.module_category.joke");

    private final String translationKey;

    ModuleCategory(String translationKey) {
        this.translationKey = translationKey;
    }

    public Component displayName() {
        return Component.translatable(translationKey);
    }
}
