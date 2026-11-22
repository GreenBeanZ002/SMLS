package com.greenbeanz.client.session;

/** Fail-closed classification returned by a response parser. */
public enum ResponseStatus {
    UNRELATED,
    PROGRESS,
    CONFIRMED,
    REJECTED
}
