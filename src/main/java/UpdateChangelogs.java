// CONVENTIONS
//================================================================================
// Commit messages: <:gitmoji:> (<modules>) <message>
// ReleaseOld commits: :bookmark: <module>@<version>,...

//================================================================================
// FLOW
//================================================================================
// - parseArgs()
// - GitReleaseCommitsCommand
// - MODE.updateChangelogs(), for modes.MultiModule:
//     - updateProjectChangelog()
//         - GitHeadHashCommand
//         - compare with lastHash from file
//         - GitCommitsBetweenCommand, commits with the (project) scope
//         - ProjectChangelogWriter.write()
//     - updateModulesChangelog()
//         - ModuleRelease.parseAll()
//         - for each module:
//             - compare hashes
//             - findPrevRelease()
//             - GitCommitsBetweenCommand, commits with the (module) scope
//             - ModuleChangelogWriter.write()
// - commit() (only if --commit arg is present)

import java.nio.file.Path;
import java.util.*;

import changelog.ChangelogOptions;
import commons.Command;
import commons.Logger.LogLevel;
import commons.Utils;
import git.Commit;
import git.GitReleaseCommitsCommand;
import git.GitRemoteUrlCommand;
import modes.MultiModule;
import modes.ProjectMode;
import modes.SingleModule;

import static commons.Logger.LOGGER;

//@formatter:off
public class UpdateChangelogs {
    static boolean COMMIT = false;
    static boolean DRY_RUN = false;
    static String REPO_URL = null;
    static ProjectMode MODE = new MultiModule();
    static Path RELEASE_NOTES = null;

    static final String COMMIT_USER = "palexdev";
    static final String COMMIT_MAIL = "alessandro.parisi406@gmail.com";
    static final String COMMIT_AUTHOR = "github-actions[bot] <github-actions[bot]@users.noreply.github.com>";

    static void main(String[] args){
        parseArgs(args);
        resolveRepoUrl();
        try {
            List<Commit> releaseCommits = new GitReleaseCommitsCommand().exec();
            MODE.updateChangelogs(releaseCommits, new ChangelogOptions(DRY_RUN, REPO_URL, RELEASE_NOTES));
        } catch (Exception ex) {
            LOGGER.error(ex);
        }
        if (COMMIT) {
            if (DRY_RUN) {
                LOGGER.info("[dry-run] Skipping commit and push, nothing was written.");
            } else {
                commit();
            }
        }
    }

    /// Resolves the repository URL once, it's used to turn commit hashes into links.
    ///
    /// Not being able to resolve it is not fatal, hashes just fall back to plain text.
    static void resolveRepoUrl() {
        try {
            REPO_URL = new GitRemoteUrlCommand().exec();
            LOGGER.debug("Repository URL: " + REPO_URL);
        } catch (Exception ex) {
            LOGGER.warn("Could not resolve the repository URL, commit hashes won't be links.");
        }
    }

    static void commit() {
        try {
            LOGGER.info("Committing and pushing changes...");
            Command.command("git", "config", "user.name", COMMIT_USER).exec();
            Command.command("git", "config", "user.email", COMMIT_MAIL).exec();
            LOGGER.debug("Configured git...");
            Command.command("git", "add", "changelogs/*").exec();
            LOGGER.debug("Added all files...");
            Command.command(
                "git", "commit",
                "-m", ":memo: (project) Updated changelogs!",
                "--author=%s".formatted(COMMIT_AUTHOR)
            ).exec();
            LOGGER.debug("Committed changes...");
            Command.command("git", "push").exec();
            LOGGER.debug("Pushed changes...");
        } catch (Exception ex) {
            LOGGER.error(ex);
        }
    }

    static void parseArgs(String[] args) {
        NavigableSet<String> toArgsSet = new TreeSet<>(Arrays.asList(args));
        if (toArgsSet.contains("--help") || toArgsSet.contains("-h")) {
            printHelp();
            System.exit(0);
        }
        if (toArgsSet.contains("--commit")) {
            COMMIT = true;
        }
        if (toArgsSet.contains("--dry-run")) {
            DRY_RUN = true;
        }
        if (toArgsSet.contains("--single")) {
            MODE = new SingleModule();
        }

        String notesArg = toArgsSet.ceiling("--release-notes=");
        if (notesArg != null && notesArg.startsWith("--release-notes=")) {
            RELEASE_NOTES = Path.of(notesArg.split("=", 2)[1]);
        }

        String logArg = toArgsSet.ceiling("--log-level=");
        if (logArg != null && logArg.startsWith("--log-level=")) {
            String level = logArg.split("=", 2)[1];
            LOGGER.atLevel(LogLevel.valueOf(level.toUpperCase()));
        }

        String transitioningArg = toArgsSet.ceiling("--transitioning");
        if ("--transitioning".equals(transitioningArg)) {
            System.setProperty(Utils.TRANSITIONING, "true");
        }
    }

    static void printHelp() {
        String message = """
            This script is responsible for updating the changelogs of the project and its modules.
            It fetches all the commits between the current release and the previous release. Then, for each module in
            the current release, takes the related commits, categorizes them and appends their header to the changelog.
            
            ** Conventions & Standards**
            - Release commits are identified by the :bookmark: gitmoji
            - To indicate to which module the commit belongs to, it should be indicated in the header as (<module1>, <module2>...)
            - The gitmoji in the commit tells to which category the changes belong to. If the gitmoji is not present
              or not recognized, it falls back to the Misc category.
            - Commit hashes are linked to their commit page, using the URL of the 'origin' remote.
              If the remote cannot be resolved, they fall back to the plain <commit hash> form.
            - Modules changelogs have this format:
              ## <release version> - <release date> - <release commit hash>
              - [`<commit hash>`](<repo url>/commit/<commit hash>) <header message>
            - Project changelog has this format:
              ## Project - <release commit hash>
              - <commit date> [`<commit hash>`](<repo url>/commit/<commit hash>) <header message>
            - Dates are in the format of my country, sorry americans: dd-MM-yyyy
            
            ** Arguments **
              • --help, -h          : Prints this help message.
              • --commit            : Updates the changelogs, makes a commit and pushes to remote
              • --dry-run           : Prints the changelog entries to the console without writing any file
                                      (nothing is committed or pushed either, even with --commit)
              • --single            : Single-module project: commits have no (module) scope, the release commit is
                                      ":bookmark: <text> <version>", and the notes of the latest release are written
                                      to --release-notes instead of the changelogs folder
              • --release-notes=<file> : Where --single writes the release notes
              • --log-level=<level> : Enables logging message. Available levels are TRACE, DEBUG, INFO, WARN, ERROR (by default, WARN)
            """;
        System.out.println(message);
    }
}
