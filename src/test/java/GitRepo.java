import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

final class GitRepo {
    static final String REPO_URL = "https://github.com/test/repo";
    private static final Path CLASSES = Path.of("target", "classes").toAbsolutePath();
    private static final String JAVA = ProcessHandle.current().info().command().orElse("java");
    private static final boolean WINDOWS = System.getProperty("os.name").startsWith("Windows");

    final Path dir;

    GitRepo(Path dir) throws Exception {
        this.dir = dir;
        exec("git", "init", "-q", "-b", "main");
        exec("git", "config", "user.name", "Test");
        exec("git", "config", "user.email", "test@example.com");
        exec("git", "config", "commit.gpgsign", "false");
        exec("git", "remote", "add", "origin", REPO_URL + ".git");
    }

    String commit(String message) throws Exception {
        exec("git", "commit", "-q", "--allow-empty", "-m", message);
        return exec("git", "log", "-1", "--pretty=format:%h").trim();
    }

    Result run(String mainClass, String... args) throws Exception {
        String nativeDir = System.getProperty("devscripts.native");
        List<String> command = new ArrayList<>(nativeDir != null
            ? List.of(Path.of(nativeDir, mainClass + (WINDOWS ? ".exe" : "")).toAbsolutePath().toString())
            : List.of(JAVA, "-cp", CLASSES.toString(), mainClass));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).directory(dir.toFile()).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return new Result(process.waitFor(), output);
    }

    String read(String path) throws IOException {
        return Files.readString(dir.resolve(path), StandardCharsets.UTF_8);
    }

    boolean exists(String path) {
        return Files.exists(dir.resolve(path));
    }

    static String today() {
        return String.format("%1$td/%1$tm/%1$tY", new Date());
    }

    static String link(String hash) {
        return "[`%s`](%s/commit/%s)".formatted(hash, REPO_URL, hash);
    }

    private String exec(String... command) throws Exception {
        Process process = new ProcessBuilder(command).directory(dir.toFile()).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.waitFor() != 0) throw new IllegalStateException(String.join(" ", command) + " failed: " + output);
        return output;
    }

    record Result(int exitCode, String output) {}
}
