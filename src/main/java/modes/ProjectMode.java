package modes;

import java.util.List;

import changelog.ChangelogOptions;
import git.Commit;
import git.ModuleRelease;

public interface ProjectMode {
    List<ModuleRelease> releases(Commit releaseCommit);

    void updateChangelogs(List<Commit> releaseCommits, ChangelogOptions options);

    String publishTask(ModuleRelease release);
}
