package changelog;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import git.Commit;

public class ProjectChangelogWriter extends ChangelogWriter {
    private final String hash;

    public ProjectChangelogWriter(Path path, String hash, ChangelogOptions options) {
        super(path, options);
        this.hash = hash;
    }

    @Override
    protected String buildHeader() {return "## %s - %s\n".formatted("Project", hash);}

    @Override
    protected String formatCommit(Commit commit) {
        String message = commit.header()
            .replaceFirst("^:\\w+:\\s*", "") // strips gitmoji
            .replaceFirst("\\(project\\)", "") // strips project tag
            .trim();
        return "- %s %s %s".formatted(commit.date(), Entries.formatHash(commit.hash(), options.repoUrl()), message);
    }

    @Override
    protected List<String> buildEntry(List<Commit> commits) {
        List<String> entry = new ArrayList<>();
        entry.add(buildHeader());
        commits.stream().map(this::formatCommit).forEach(entry::add);
        entry.add("\n\n");
        return entry;
    }
}
