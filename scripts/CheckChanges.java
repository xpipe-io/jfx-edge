// Usage: java scripts/CheckChanges.java <configuration file> <force> >> "$GITHUB_OUTPUT"

final Path HERE = Path.of(".");

String resolve(Config.Link link) throws IOException, InterruptedException {
    if (link.kind().equals("commit")) {
        return link.ref();
    }
    // Like git, prefer a tag over a branch, and the commit (^{}) of an annotated tag over the tag object
    String ref = link.ref();
    List<String> candidates = link.kind().equals("pull")
            ? List.of("refs/pull/" + ref + "/head")
            : List.of("refs/tags/" + ref + "^{}", "refs/tags/" + ref, "refs/heads/" + ref);

    List<String> cmd = new ArrayList<>(List.of("git", "ls-remote", link.repoUrl()));
    cmd.addAll(candidates);
    Map<String, String> shas = new HashMap<>();
    for (String line : Shell.exec(HERE, cmd.toArray(String[]::new)).lines().toList()) {
        String[] shaAndRef = line.split("\\s+");
        shas.put(shaAndRef[1], shaAndRef[0]);
    }
    return candidates.stream().filter(shas::containsKey).map(shas::get).findFirst()
            .orElseThrow(() -> new IllegalArgumentException(link.url() + " does not exist"));
}

String buildTag(Config.Link base) throws IOException, InterruptedException {
    Path dir = Files.createTempDirectory("base");
    Shell.exec(HERE, "git", "clone", "--quiet", "--filter=blob:none", "--no-checkout", base.repoUrl(), dir.toString());
    Shell.exec(dir, "git", "fetch", "--quiet", "origin", base.fetchRef());
    return Shell.exec(dir, "git", "describe", "--tags", "--abbrev=0", "--match", "*+*", "FETCH_HEAD");
}

List<String> snapshotState(Config config) throws IOException, InterruptedException {
    List<String> state = new ArrayList<>();
    state.add("base " + config.base().url() + " " + resolve(config.base()));
    for (Config.Link link : config.mergeList()) {
        state.add("merge " + link.url() + " " + resolve(link));
    }
    state.add("build-props " + config.buildProps());
    state.add("platforms " + config.platforms());
    state.add("boot-jdk " + config.bootJdkDistribution() + " " + config.bootJdkVersion());
    state.add("tests " + config.tests());
    state.add("maven-repository " + config.mavenRepository());
    state.add("runners " + config.runners());
    state.add("build-files " + Shell.exec(HERE, "git", "rev-parse", "HEAD:.github", "HEAD:scripts", "HEAD:maven")
            .replace("\n", " "));
    return state;
}

String previousState(Config config) throws IOException, InterruptedException {
    Optional<String> tag = Shell.exec(HERE, "gh", "release", "list", "--limit", "100", "--json", "tagName", "--jq", ".[].tagName")
            .lines()
            .filter(t -> t.contains("-" + config.name() + "+"))
            .findFirst();
    if (tag.isEmpty()) {
        return "";
    }
    Shell.exec(HERE, "gh", "release", "download", tag.get(), "--pattern", "build-state.txt", "--output", "previous-build-state.txt");
    return Files.readString(Path.of("previous-build-state.txt"));
}

void main(String[] args) throws Exception {
    Config config = Config.read(Path.of(args[0]));
    boolean force = Boolean.parseBoolean(args[1]);

    List<String> lines = config.snapshot()
            ? snapshotState(config)
            : List.of("release " + config.base().repoUrl() + " " + buildTag(config.base()));
    String state = String.join("\n", lines) + "\n";
    Files.writeString(Path.of("build-state.txt"), state);
    System.err.print(state);

    boolean changed = !state.equals(previousState(config)) || (force && config.snapshot());
    System.out.println("changed=" + changed);
}
