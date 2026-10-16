package modes;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import changelog.ChangelogOptions;
import changelog.ModuleChangelogWriter;
import changelog.ProjectChangelogWriter;
import commons.Utils;
import git.Commit;
import git.GitCommitsBetweenCommand;
import git.GitHeadHashCommand;
import git.ModuleRelease;

import static commons.Logger.LOGGER;
import static commons.Utils.findPrevGitRelease;

public class MultiModule implements ProjectMode {
    public static final Path CHANGELOGS_PATH = Paths.get("changelogs");

    @Override
    public List<ModuleRelease> releases(Commit releaseCommit) {
        return ModuleRelease.parseAll(releaseCommit);
    }

    @Override
    public void updateChangelogs(List<Commit> releaseCommits, ChangelogOptions options) {
        updateProjectChangelog(options);
        updateModulesChangelog(releaseCommits, options);
    }

    @Override
    public String publishTask(ModuleRelease release) {
        return ":%s:publishAndReleaseToMavenCentral".formatted(release.module());
    }

    private void updateProjectChangelog(ChangelogOptions options) {
        LOGGER.info("Updating project changelog...");
        try {
            String headHash = new GitHeadHashCommand().exec();
            var writer = new ProjectChangelogWriter(CHANGELOGS_PATH.resolve("project.md"), headHash, options);
            String lastHash = writer.readLastHash();

            if (headHash.equals(lastHash)) {
                LOGGER.info("Project changelog is up to date.");
                return;
            }

            List<Commit> commits = new GitCommitsBetweenCommand(lastHash, headHash).exec().stream()
                .filter(c -> c.hasScope("project"))
                .filter(c -> !c.header().contains("Updated changelogs"))
                .toList();
            if (commits.isEmpty()) {
                LOGGER.info("No changes found for project, skipping...");
                return;
            }

            LOGGER.info("Found %d commits since last update, updating...".formatted(commits.size()));
            writer.write(commits);
        } catch (Exception ex) {
            LOGGER.error(ex);
        }
    }

    private void updateModulesChangelog(List<Commit> releaseCommits, ChangelogOptions options) {
        LOGGER.info("Updating modules changelogs...");
        try {
            if (releaseCommits.isEmpty()) {
                LOGGER.warn("No release commits found.");
                return;
            }

            List<ModuleRelease> releases = releases(releaseCommits.getFirst());
            LOGGER.info("Found releases: " + String.join(",", releases.stream().map(ModuleRelease::module).toList()));
            for (ModuleRelease release : releases) {
                String module = release.module();
                LOGGER.trace("Processing module: " + module);
                Path path = CHANGELOGS_PATH.resolve(module + ".md");
                var writer = new ModuleChangelogWriter(path, release, options);
                String lastHash = writer.readLastHash();
                if (release.commit().hash().equals(lastHash)) {
                    LOGGER.info("Module changelog for %s is up to date, skipping...".formatted(module));
                    continue;
                }

                String prevReleaseHash = findPrevGitRelease(module, releaseCommits, releaseCommits.getFirst());
                if (Boolean.getBoolean(Utils.TRANSITIONING) && prevReleaseHash == null) {
                    LOGGER.warn("Could not find previous release for module %s".formatted(module));
                    prevReleaseHash = IO.readln("Prev release hash: ").trim();
                    if (prevReleaseHash.isBlank()) {
                        LOGGER.error("Invalid hash, skipping...");
                        continue;
                    }
                }
                List<Commit> commits = new GitCommitsBetweenCommand(prevReleaseHash, release.commit().hash()).exec().stream()
                    .filter(c -> c.hasScope(module))
                    .toList();
                if (commits.isEmpty()) {
                    LOGGER.info("No changes found for module %s, skipping...".formatted(module));
                    continue;
                }

                LOGGER.info("Found %d commits for module %s since last update, updating...".formatted(commits.size(), module));
                writer.write(commits);
            }
        } catch (Exception ex) {
            LOGGER.error(ex);
        }
    }
}
