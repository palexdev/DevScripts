package git;

import java.time.Instant;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import commons.Utils;

public record Commit(
    long timestamp,
    String hash,
    String header
) {
    private static final Pattern SCOPE_PATTERN = Pattern.compile("\\(([^)]+)\\)");

    public String date() {
        return Utils.instantToDate(Instant.ofEpochSecond(timestamp));
    }

    public boolean hasScope(String module) {
        Matcher matcher = SCOPE_PATTERN.matcher(header);
        return matcher.find() && Arrays.stream(matcher.group(1).split(","))
            .map(String::trim)
            .anyMatch(module::equals);
    }
}
