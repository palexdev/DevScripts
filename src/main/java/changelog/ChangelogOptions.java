package changelog;

import java.nio.file.Path;

public record ChangelogOptions(boolean dryRun, String repoUrl, Path releaseNotes) {}
