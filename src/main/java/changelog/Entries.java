package changelog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import git.Commit;
import git.GitMojis;
import git.GitMojis.Category;

public final class Entries {
    private Entries() {}

    /// Formats the commit hash as a Markdown link to its commit page on GitHub.
    ///
    /// Falls back to the plain `<hash>` form when the repository URL could not be resolved.
    public static String formatHash(String hash, String repoUrl) {
        if (repoUrl == null || repoUrl.isBlank()) return "<%s>".formatted(hash);
        return "[`%s`](%s/commit/%s)".formatted(hash, repoUrl, hash);
    }

    public static List<String> categorized(String header, List<Commit> commits, Function<Commit, String> format) {
        List<String> entry = new ArrayList<>();
        entry.add(header);
        Map<Category, List<Commit>> categorized = GitMojis.categorize(commits);
        for (Category category : GitMojis.CATEGORIES) {
            List<Commit> byCat = categorized.get(category);
            if (byCat == null || byCat.isEmpty()) continue;
            entry.add("\n### %s\n".formatted(category.name()));
            byCat.stream().map(format).forEach(entry::add);
        }
        entry.add("\n\n");
        return entry;
    }
}
