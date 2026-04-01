package git;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import commons.Utils;

import static commons.Logger.LOGGER;

public record ModuleRelease(String module, String version, Commit commit) {
    private static final Pattern RELEASE_PATTERN = Pattern.compile("([\\w-]+)@([\\w.]+)");

    /// Parses a list of `ModuleReleases` from a release commit.
    ///
    /// The release commit header is expected to follow the convention: `:bookmark: module@version, module2@version2, ...`
    ///
    /// The `:bookmark:` gitmoji is stripped first, then the remaining string is split by `,` and
    /// each token is split by `@` to extract the module name and version.
    ///
    /// Malformed tokens (i.e. not containing exactly one `@`) are skipped and logged as errors.
    ///
    /// @param releaseCommit the release commit to parse, identified by the `:bookmark:` gitmoji
    public static List<ModuleRelease> parseAll(Commit releaseCommit) {
        List<ModuleRelease> releases = new ArrayList<>();
        Matcher matcher = RELEASE_PATTERN.matcher(releaseCommit.header());
        while (matcher.find()) {
            String module = matcher.group(1);
            String version = matcher.group(2);
            releases.add(new ModuleRelease(module, version, releaseCommit));
        }

        if (releases.isEmpty()) {
            if (Boolean.getBoolean(Utils.TRANSITIONING)) {
                ModuleRelease oldFormatMr = handleOldFormat(releaseCommit);
                if (oldFormatMr != null) {
                    releases.add(oldFormatMr);
                    return releases;
                }
            }
            LOGGER.error("Invalid release commit: " + releaseCommit.header());
        }

        return releases;
    }

    private static ModuleRelease handleOldFormat(Commit releaseCommit) {
        String module = IO.readln("Module: ").trim();
        String version = IO.readln("Version: ").trim();
        if (module.isBlank() || version.isBlank()) return null;
        return new ModuleRelease(module, version, releaseCommit);
    }
}
