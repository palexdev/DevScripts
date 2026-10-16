import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiModuleTest {

    @Test
    void firstReleaseWritesModuleAndProjectChangelogs(@TempDir Path dir) throws Exception {
        GitRepo repo = new GitRepo(dir);
        String parser = repo.commit(":sparkles: (core) Add parser");
        String crash = repo.commit(":bug: (core, ui) Fix crash");
        String docs = repo.commit(":memo: (ui) Document the button");
        String build = repo.commit(":recycle: (project) Tidy the build");
        repo.commit(":memo: (project) Updated changelogs!");
        String release = repo.commit(":bookmark: core@1.0.0, ui@2.0.0");

        assertEquals(0, repo.run("UpdateChangelogs").exitCode());

        assertEquals(String.join("\n", List.of(
            "## 1.0.0 - %s - %s".formatted(GitRepo.today(), release),
            "\n### Features\n",
            "- %s Add parser".formatted(GitRepo.link(parser)),
            "\n### Bug Fixes\n",
            "- %s Fix crash".formatted(GitRepo.link(crash)),
            "\n\n"
        )), repo.read("changelogs/core.md"));

        assertEquals(String.join("\n", List.of(
            "## 2.0.0 - %s - %s".formatted(GitRepo.today(), release),
            "\n### Bug Fixes\n",
            "- %s Fix crash".formatted(GitRepo.link(crash)),
            "\n### Documentation\n",
            "- %s Document the button".formatted(GitRepo.link(docs)),
            "\n\n"
        )), repo.read("changelogs/ui.md"));

        assertEquals(String.join("\n", List.of(
            "## Project - %s%n".formatted(release),
            "- %s %s Tidy the build".formatted(GitRepo.today(), GitRepo.link(build)),
            "\n\n"
        )), repo.read("changelogs/project.md"));
    }

    @Test
    void nextReleaseTakesOnlyTheCommitsSinceThatModulesPreviousRelease(@TempDir Path dir) throws Exception {
        GitRepo repo = new GitRepo(dir);
        repo.commit(":sparkles: (core) Add parser");
        repo.commit(":sparkles: (ui) Add button");
        repo.commit(":bookmark: core@1.0.0, ui@2.0.0");
        repo.run("UpdateChangelogs");
        String uiBefore = repo.read("changelogs/ui.md");

        String leak = repo.commit(":bug: (core) Fix leak");
        repo.commit(":sparkles: (ui) Add slider");
        String release = repo.commit(":bookmark: core@1.1.0");
        assertEquals(0, repo.run("UpdateChangelogs").exitCode());

        String core = repo.read("changelogs/core.md");
        assertTrue(core.startsWith(String.join("\n", List.of(
            "## 1.1.0 - %s - %s".formatted(GitRepo.today(), release),
            "\n### Bug Fixes\n",
            "- %s Fix leak".formatted(GitRepo.link(leak)),
            "\n\n"
        ))), core);
        assertTrue(core.contains("## 1.0.0 - "), core);
        assertFalse(core.contains("Add slider"), core);
        assertEquals(uiBefore, repo.read("changelogs/ui.md"));
    }

    @Test
    void runningTwiceChangesNothing(@TempDir Path dir) throws Exception {
        GitRepo repo = new GitRepo(dir);
        repo.commit(":sparkles: (core) Add parser");
        repo.commit(":recycle: (project) Tidy the build");
        repo.commit(":bookmark: core@1.0.0");

        repo.run("UpdateChangelogs");
        String core = repo.read("changelogs/core.md");
        String project = repo.read("changelogs/project.md");
        repo.run("UpdateChangelogs");

        assertEquals(core, repo.read("changelogs/core.md"));
        assertEquals(project, repo.read("changelogs/project.md"));
    }

    @Test
    void qualifiedVersionsAreKept(@TempDir Path dir) throws Exception {
        GitRepo repo = new GitRepo(dir);
        repo.commit(":sparkles: (core) Add parser");
        String release = repo.commit(":bookmark: core@25.1.0-EA1");

        repo.run("UpdateChangelogs");

        assertTrue(repo.read("changelogs/core.md").startsWith("## 25.1.0-EA1 - %s - %s".formatted(GitRepo.today(), release)));
    }

    @Test
    void releaseDryRunListsTheModulesOfTheLastReleaseCommit(@TempDir Path dir) throws Exception {
        GitRepo repo = new GitRepo(dir);
        repo.commit(":sparkles: (core) Add parser");
        repo.commit(":bookmark: core@1.0.0, ui@2.0.0");

        GitRepo.Result result = repo.run("Release", "--dry-run");

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("  - core@1.0.0"), result.output());
        assertTrue(result.output().contains("  - ui@2.0.0"), result.output());
        assertFalse(repo.exists("changelogs"));
    }
}
