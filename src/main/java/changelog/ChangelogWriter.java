package changelog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import git.Commit;

import static commons.Logger.LOGGER;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

public abstract class ChangelogWriter {
    protected final Path path;
    protected final ChangelogOptions options;

    protected ChangelogWriter(Path path, ChangelogOptions options) {
        this.path = path;
        this.options = options;
    }

    protected abstract String buildHeader();
    protected abstract String formatCommit(Commit commit);
    /// Builds the block of lines to prepend to the changelog
    protected abstract List<String> buildEntry(List<Commit> commits);

    /// Prepends the entry built by [#buildEntry(List)] to the changelog file.
    ///
    /// In dry-run mode nothing is touched on disk, the entry is just printed.
    public void write(List<Commit> commits) throws IOException {
        List<String> toPrepend = buildEntry(commits);
        if (options.dryRun()) {
            LOGGER.info("[dry-run] %s would become:%n%s".formatted(path, String.join("\n", toPrepend).strip()));
            return;
        }
        ensureFileExists();
        List<String> content = new LinkedList<>(Files.readAllLines(path, StandardCharsets.UTF_8));
        content.addAll(0, toPrepend);
        Files.writeString(path, String.join("\n", content), StandardCharsets.UTF_8, TRUNCATE_EXISTING);
    }

    /// Reads the last `##` header line from the changelog file, hash is the last segment
    public String readLastHash() throws IOException {
        if (!Files.exists(path)) return null;
        List<String> allLines = Files.readAllLines(path, StandardCharsets.UTF_8);
        return allLines.stream()
            .filter(l -> l.startsWith("## "))
            .findFirst()
            .map(l -> l.split(" - "))
            .map(parts -> parts[parts.length - 1].trim())
            .orElse(null);
    }

    protected void ensureFileExists() throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
        }
    }
}
