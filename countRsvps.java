///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.6.3
//DEPS org.kohsuke:github-api:1.318

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

@Command(name = "countRsvps", mixinStandardHelpOptions = true, version = "countRsvps 0.1",
        description = "countRsvps made with jbang")
class countRsvps implements Callable<Integer> {

    @Parameters(index = "0", description = "The GitHub token", defaultValue = "${env:GITHUB_TOKEN}")
    private String GITHUB_TOKEN;

    @Parameters(index = "1", description = "The GitHub repo for the Issue", defaultValue = "${env:GITHUB_REPO}")
    private String GITHUB_REPO;

    @Parameters(index = "2", description = "The GitHub Issue with RSVP comments", defaultValue = "${env:GITHUB_ISSUE_NUMBER}")
    private Integer GITHUB_ISSUE_NUMBER;

    @Option(names = { "-f", "--force" }, description = "Force commenting in any case", defaultValue = "false")
    private boolean FORCE;

    public static void main(String... args) {
        int exitCode = new CommandLine(new countRsvps()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        System.out.println("Will interact with: "+GITHUB_REPO+"/ "+GITHUB_ISSUE_NUMBER);
        GitHub github = new GitHubBuilder().withOAuthToken(GITHUB_TOKEN).build();
        var issue = github.getRepository(GITHUB_REPO).getIssue(GITHUB_ISSUE_NUMBER);
        Set<String> rsvp_yes = new LinkedHashSet<>();
        var lastCommentBody = issue.getComments().getLast().getBody().toLowerCase();
        if (!lastCommentBody.startsWith("/rsvp-") && !FORCE) {
            System.out.println("Last comment is not a command, early return.");
            return 0;
        }
        for (GHIssueComment c : issue.getComments()) {
            String userNameLogin = c.getUser().getName() + " (" + c.getUser().getLogin() + ")";
            String commentBody = c.getBody();
            String bodyLower = commentBody.toLowerCase();
            if (bodyLower.startsWith("/rsvp-yes")) {
                rsvp_yes.add(userNameLogin);
            } else if (commentBody.toLowerCase().startsWith("/rsvp-no")) {
                rsvp_yes.remove(userNameLogin);
            }
        }
        StringBuilder sb = new StringBuilder("In RSVP Yes:").append("\n");
        for (String u : rsvp_yes) {
            sb.append("- ").append(u).append("\n");
        }
        sb.append("\n").append("Total: ").append(rsvp_yes.size()).append(".");
        System.out.println("Will comment totals...");
        issue.comment(sb.toString());
        System.out.println("Commented.");
        return 0;
    }
}
