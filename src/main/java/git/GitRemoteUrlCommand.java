package git;

import commons.Command;

/// Retrieves the `origin` remote URL, normalized to a browsable `https` URL.
///
/// SSH remotes are converted (`git@host:owner/repo.git` -> `https://host/owner/repo`), and the
/// trailing `.git` is always stripped, so the result can be used as a base for commit links.
public class GitRemoteUrlCommand extends Command<String> {
    @Override
    protected String[] args() {return new String[]{"git", "remote", "get-url", "origin"};}

    @Override
    protected String parse(String output) {
        String url = output.trim();
        if (url.isBlank()) return null;
        if (url.startsWith("ssh://git@")) {
            url = url.replaceFirst("^ssh://git@", "https://");
        } else if (url.startsWith("git@")) {
            url = url.replaceFirst("^git@([^:]+):", "https://$1/");
        }
        if (url.endsWith(".git")) url = url.substring(0, url.length() - ".git".length());
        return url;
    }
}
