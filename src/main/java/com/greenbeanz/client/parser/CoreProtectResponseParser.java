package com.greenbeanz.client.parser;

import java.util.Locale;
import java.util.regex.Pattern;

/** Matching is tolerant of CoreProtect prefix styles and trailing text (e.g. `for "<user>"`). */
public final class CoreProtectResponseParser {
    /** Optional "[CoreProtect]" / "CoreProtect -" / "CoreProtect:" prefix. */
    private static final String PREFIX = "^(?:\\[coreprotect]|coreprotect\\s*[-\u2013:])?\\s*";

    private static final Pattern REJECTED = Pattern.compile(
            PREFIX + "(?:error|rollback failed|restore failed|no permission"
                    + "|a command is currently being processed)\\b.*$",
            Pattern.CASE_INSENSITIVE);

    /** Nothing to roll back: the command finished, so the chain should continue. */
    private static final Pattern NOTHING_FOUND = Pattern.compile(
            PREFIX + "(?:no data found|nothing found|no blocks found)\\b.*$",
            Pattern.CASE_INSENSITIVE);

    private CoreProtectResponseParser() {
    }

    public static Result parse(String cleanText) {
        return parse("rollback", cleanText);
    }

    /** Parses only completion for the expected destructive action; unrelated success text never advances it. */
    public static Result parse(String expectedAction, String cleanText) {
        String action = expectedAction == null ? "" : expectedAction.trim().toLowerCase(Locale.ROOT);
        String value = cleanText == null ? "" : cleanText.trim().toLowerCase(Locale.ROOT);
        if (REJECTED.matcher(value).matches()) return Result.REJECTED;
        if (action.equals("rollback") || action.equals("restore")) {
            Pattern done = Pattern.compile(PREFIX + Pattern.quote(action) + " complete(?:d)?\\b.*$");
            if (done.matcher(value).matches()) return Result.CONFIRMED;
            if (NOTHING_FOUND.matcher(value).matches()) return Result.CONFIRMED;
        }
        return Result.UNRELATED;
    }

    public enum Result { CONFIRMED, REJECTED, UNRELATED }
}