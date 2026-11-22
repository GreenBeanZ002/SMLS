package com.greenbeanz.client.instaban;

import java.net.URI;
import java.time.Instant;

public record InstaBanResult(
        String ign,
        InstaBanStatus status,
        InstaBanReason reason,
        URI messageUrl,
        Instant messageTimestamp
) {
}
