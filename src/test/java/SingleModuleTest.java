import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SingleModuleTest {

    @Test
    void firstReleaseWritesTheNotesOfAllCommits(@TempDir Path dir) throws Exception {
        GitRepo repo = new GitRepo(dir);
        String parser = repo.commit(":sparkles: Add parser");
        String crash = repo.commit(":bug: Fix crash");
        String docs = repo.commit(":memo: Add the README");
        repo.commit(":bookmark: Release version 25.1.0");

        assertEquals(0, repo.run("UpdateChangelogs", "--single", "--release-notes=notes.md").exitCode());

        assertEquals("""
            ### Features

            - %s Add parser

            ### Bug Fixes

            - %s Fix crash

            ### Documentation

            - %s Add the README""".formatted(GitRepo.link(parser), GitRepo.link(crash), GitRepo.link(docs)),
            repo.read("notes.md"));
        assertFalse(repo.exists("changelogs"));
    }

    @Test
    void nextReleaseTakesOnlyTheCommitsSinceThePreviousRelease(@TempDir Path dir) throws Exception {
        GitRepo repo = new GitRepo(dir);
        repo.commit(":sparkles: Add parser");
        repo.commit(":bookmark: Release v25.1.0");
        String leak = repo.commit(":bug: Fix leak");
        repo.commit(":bookmark: Release v25.1.1");

        repo.run("UpdateChangelogs", "--single", "--release-notes=notes.md");

        assertEquals("""
            ### Bug Fixes

            - %s Fix leak""".formatted(GitRepo.link(leak)), repo.read("notes.md"));
    }

    @Test
    void releaseParsesTheVersionAndPublishesTheRootProject(@TempDir Path dir) throws Exception {
        GitRepo repo = new GitRepo(dir);
        repo.commit(":sparkles: Add parser");

        for (String header : new String[]{":bookmark: 25.2.0-EA", ":bookmark: Release version 25.2.0-EA", ":bookmark: Release v25.2.0-EA"}) {
            repo.commit(header);
            GitRepo.Result result = repo.run("Release", "--single", "--dry-run");
            assertEquals(0, result.exitCode());
            assertTrue(result.output().contains("  - project@25.2.0-EA (publishAndReleaseToMavenCentral)"), header + "\n" + result.output());
        }
    }

    @Test
    void multiModuleReleaseNamesTheModuleTask(@TempDir Path dir) throws Exception {
        GitRepo repo = new GitRepo(dir);
        repo.commit(":sparkles: (core) Add parser");
        repo.commit(":bookmark: core@1.0.0");

        GitRepo.Result result = repo.run("Release", "--dry-run");

        assertTrue(result.output().contains("  - core@1.0.0 (:core:publishAndReleaseToMavenCentral)"), result.output());
    }
}
