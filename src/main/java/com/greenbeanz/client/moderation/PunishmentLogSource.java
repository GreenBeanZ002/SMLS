package com.greenbeanz.client.moderation;

import java.util.List;

@FunctionalInterface
public interface PunishmentLogSource {
    PunishmentLogSource EMPTY = List::of;

    List<PunishmentLogEntry> latest();
}
