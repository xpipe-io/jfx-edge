// Usage: java scripts/MergePrs.java <configuration file> <checkout dir> <report file>

String git(Path dir, String... args) throws IOException, InterruptedException {
    List<String> cmd = new ArrayList<>(List.of("git"));
    cmd.addAll(List.of(args));
    return Shell.exec(dir, cmd.toArray(String[]::new));
}

String describe(Config.Link link) throws IOException, InterruptedException {
    return switch (link.kind()) {
        case "pull" -> {
            String title = Shell.exec(Path.of("."), "gh", "api",
                    "repos/%s/%s/pulls/%s".formatted(link.owner(), link.repo(), link.ref()), "--jq", ".title");
            yield "PR #%s: \"%s\" (%s/%s)".formatted(link.ref(), title, link.owner(), link.repo());
        }
        case "tree" -> "branch %s/%s@%s".formatted(link.owner(), link.repo(), link.ref());
        default -> "commit %s/%s@%s".formatted(link.owner(), link.repo(), link.ref());
    };
}

void main(String[] args) throws Exception {
    Config config = Config.read(Path.of(args[0]));
    Path dir = Path.of(args[1]);
    Config.Link base = config.base();

    git(Path.of("."), "clone", base.repoUrl(), dir.toString());
    git(dir, "fetch", "origin", base.fetchRef());
    git(dir, "checkout", "--detach", "FETCH_HEAD");
    git(dir, "config", "user.name", "jfx-openbuild-bot");
    git(dir, "config", "user.email", "noreply@github.com");

    List<String> report = new ArrayList<>();
    if (config.snapshot()) {
        report.add("Base: %s (`%s`), closest JavaFX tag `%s`".formatted(
                base.url(), git(dir, "rev-parse", "--short=12", "HEAD"), git(dir, "describe", "--tags", "--abbrev=0")));
    } else {
        String buildTag = git(dir, "describe", "--tags", "--abbrev=0", "--match", "*+*");
        git(dir, "checkout", "--detach", buildTag);
        report.add("Base: JavaFX tag `%s` of %s (`%s`)".formatted(buildTag, base.url(), git(dir, "rev-parse", "--short=12", "HEAD")));
    }
    report.add("");
    report.add("### Merged");

    for (Config.Link link : config.mergeList()) {
        git(dir, "fetch", link.repoUrl(), link.fetchRef());
        Shell.Result merge = Shell.run(dir, "git", "merge", "--no-ff", "--no-edit", "FETCH_HEAD");
        if (merge.exitCode() != 0) {
            String conflicts = merge.stdout().lines().filter(line -> line.startsWith("CONFLICT")).collect(Collectors.joining("; "));
            System.err.println("::error::Failed to merge " + link.url() + ": " + (conflicts.isEmpty() ? merge.stderr() : conflicts));
            System.exit(1);
        }
        report.add("- %s - %s (`%s`)".formatted(describe(link), link.url(), git(dir, "rev-parse", "--short=12", "HEAD")));
    }
    if (config.mergeList().isEmpty()) {
        report.add("- Nothing, this is the plain base");
    }

    Files.writeString(Path.of(args[2]), String.join("\n", report) + "\n");
    System.out.println(String.join("\n", report));
}
