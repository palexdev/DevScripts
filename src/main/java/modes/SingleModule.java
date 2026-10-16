package modes;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import changelog.ChangelogOptions;
import changelog.Entries;
import git.Commit;
import git.GitCommitsBetweenCommand;
import git.ModuleRelease;

import static commons.Logger.LOGGER;

public class SingleModule implements ProjectMode {
    public static final String MODULE = "project";
    private static final Pattern VERSION_PATTERN = Pattern.compile("\\bv?(\\d+(?:\\.\\d+)+(?:[-+][\\w.+-]*)?)");

    @Override
    public List<ModuleRelease> releases(Commit releaseCommit) {
        Matcher matcher = VERSION_PATTERN.matcher(releaseCommit.header().replace(":bookmark:", ""));
        if (!matcher.find()) {
            LOGGER.error("Invalid release commit, no version found: " + releaseCommit.header());
            return List.of();
        }
        return List.of(new ModuleRelease(MODULE, matcher.group(1), releaseCommit));
    }

    @Override
    public void updateChangelogs(List<Commit> releaseCommits, ChangelogOptions options) {
        LOGGER.info("Writing release notes...");
        try {
            if (releaseCommits.isEmpty()) {
                LOGGER.warn("No release commits found.");
                return;
            }
            if (options.releaseNotes() == null && !options.dryRun()) {
                LOGGER.error("Single-module projects need --release-notes=<file>.");
                return;
            }

            List<ModuleRelease> releases = releases(releaseCommits.getFirst());
            if (releases.isEmpty()) return;
            ModuleRelease release = releases.getFirst();

            String prevReleaseHash = releaseCommits.size() > 1 ? releaseCommits.get(1).hash() : null;
            List<Commit> commits = new GitCommitsBetweenCommand(prevReleaseHash, release.commit().hash()).exec().stream()
                .filter(c -> !c.header().contains(":bookmark:"))
                .toList();
            LOGGER.info("Found %d commits for release %s".formatted(commits.size(), release.version()));

            String notes = String.join("\n", Entries.categorized("", commits, c -> formatCommit(c, options))).strip();
            if (options.dryRun()) {
                LOGGER.info("[dry-run] Release notes for %s:%n%s".formatted(release.version(), notes));
                return;
            }
            Files.writeString(options.releaseNotes(), notes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            LOGGER.error(ex);
        }
    }

    @Override
    public String publishTask(ModuleRelease release) {
        return "publishAndReleaseToMavenCentral";
    }

    private static String formatCommit(Commit commit, ChangelogOptions options) {
        String message = commit.header()
            .replaceFirst("^:\\w+:\\s*", "")
            .trim();
        return "- %s %s".formatted(Entries.formatHash(commit.hash(), options.repoUrl()), message);
    }
}
