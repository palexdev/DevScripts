package git;

import java.util.ArrayList;
import java.util.List;

import commons.Command;

import static commons.Logger.LOGGER;

/// Finds all the commits in a range, the caller decides which ones belong to a module
public class GitCommitsBetweenCommand extends Command<List<Commit>> {
    private final String from;
    private final String to;

    public GitCommitsBetweenCommand(String from, String to) {
        this.from = from;
        this.to = to;
    }

    @Override
    protected String[] args() {
        String range = from != null ? from + ".." + to : to;
        return new String[]{"git", "log", range, "--pretty=format:%at%n%h%n%s"};
    }

    @Override
    protected List<Commit> parse(String output) {
        List<Commit> commits = new ArrayList<>();
        String[] lines = output.split("\n");
        for (int i = 0; i < lines.length - 2; i += 3) {
            LOGGER.trace("Parsing commit: " + lines[i + 1]);
            long timestamp = Long.parseLong(lines[i].trim());
            String hash = lines[i + 1].trim();
            String header = lines[i + 2].trim();
            commits.add(new Commit(timestamp, hash, header));
        }
        return commits;
    }
}
