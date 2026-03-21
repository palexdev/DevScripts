package git;

import java.time.Instant;

import commons.Utils;

public record Commit(
    long timestamp,
    String hash,
    String header
) {
    public String date() {
        return Utils.instantToDate(Instant.ofEpochSecond(timestamp));
    }
}
