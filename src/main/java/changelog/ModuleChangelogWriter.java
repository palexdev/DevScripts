package changelog;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import git.Commit;
import git.ModuleRelease;

import static commons.Utils.instantToDate;

public class ModuleChangelogWriter extends ChangelogWriter {
    private final ModuleRelease release;

    public ModuleChangelogWriter(Path path, ModuleRelease release, ChangelogOptions options) {
        super(path, options);
        this.release = release;
    }

    @Override
    protected String buildHeader() {
        return "## %s - %s - %s".formatted(release.version(), instantToDate(Instant.now()), release.commit().hash());
    }

    @Override
    protected String formatCommit(Commit commit) {
        String message = commit.header()
            .replaceFirst("^:\\w+:\\s*", "") // strips gitmoji
            .replaceFirst("\\([^)]+\\)\\s*", "") // strips all modules tags
            .trim();
        return "- %s %s".formatted(Entries.formatHash(commit.hash(), options.repoUrl()), message);
    }

    @Override
    protected List<String> buildEntry(List<Commit> commits) {
        return Entries.categorized(buildHeader(), commits, this::formatCommit);
    }
}
