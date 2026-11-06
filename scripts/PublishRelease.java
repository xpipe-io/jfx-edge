// Usage: java scripts/PublishRelease.java <configuration file> <snapshot>

final Path HERE = Path.of(".");
final String REPO = System.getenv("GH_REPO");

final List<List<String>> PLATFORMS = List.of(
        List.of("linux-x64", "Linux x64"),
        List.of("linux-aarch64", "Linux aarch64"),
        List.of("osx-x64", "macOS x64"),
        List.of("osx-aarch64", "macOS aarch64"),
        List.of("windows-x64", "Windows x64"));

String gh(String... args) throws IOException, InterruptedException {
    List<String> cmd = new ArrayList<>(List.of("gh"));
    cmd.addAll(List.of(args));
    return Shell.exec(HERE, cmd.toArray(String[]::new));
}

boolean ghSucceeds(String... args) throws IOException, InterruptedException {
    List<String> cmd = new ArrayList<>(List.of("gh"));
    cmd.addAll(List.of(args));
    return Shell.run(HERE, cmd.toArray(String[]::new)).exitCode() == 0;
}

String size(Path file) throws IOException {
    return String.format(Locale.ROOT, "%.1f MB", Files.size(file) / (1024.0 * 1024.0));
}

String code(List<String> values) {
    return values.isEmpty() ? "none" : values.stream().map(v -> "`" + v + "`").collect(Collectors.joining(", "));
}

String link(String label, String tag, Path file) throws IOException {
    String url = "%s/%s/releases/download/%s/%s".formatted(System.getenv("GITHUB_SERVER_URL"), REPO,
            tag.replace("+", "%2B"), file.getFileName().toString().replace("+", "%2B"));
    return "[%s](%s) (%s)".formatted(label, url, size(file));
}

void main(String[] args) throws Exception {
    Config config = Config.read(Path.of(args[0]));
    boolean snapshot = Boolean.parseBoolean(args[1]);

    Path release = Path.of("release");
    List<Path> files;
    try (var list = Files.list(release)) {
        files = list.sorted().toList();
    }
    List<String> names = files.stream().map(f -> f.getFileName().toString()).toList();

    String version = names.stream().filter(n -> n.endsWith("_bin-sdk.zip")).findFirst().orElseThrow()
            .replaceFirst("^openjfx-", "").replaceFirst("_.*$", "");
    String mavenVersion = names.stream().filter(n -> n.matches("javafx-[0-9].*\\.pom")).findFirst().orElseThrow()
            .replaceFirst("^javafx-", "").replaceFirst("\\.pom$", "");

    String tag = version;
    if (snapshot) {
        tag = version + "-SNAPSHOT";
        // Immutable releases can't be replaced, so then snapshots get the time of the build in their tag
        if (gh("api", "repos/" + REPO + "/releases?per_page=1", "--jq", ".[0].immutable").equals("true")) {
            tag = tag + "-" + DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss").format(LocalDateTime.now(ZoneOffset.UTC));
        } else if (ghSucceeds("release", "view", tag)) {
            gh("release", "delete", tag, "--yes", "--cleanup-tag");
        }
    }

    List<String> notes = new ArrayList<>();
    notes.add("### Downloads");
    notes.add("");
    notes.add("| Platform | SDK | jmods | Maven jars |");
    notes.add("|----------|-----|-------|------------|");
    for (List<String> platform : PLATFORMS) {
        String prefix = "openjfx-" + version + "_" + platform.get(0) + "_bin";
        if (Files.exists(release.resolve(prefix + "-sdk.zip"))) {
            notes.add("| %s | %s | %s | %s |".formatted(platform.get(1),
                    link("SDK", tag, release.resolve(prefix + "-sdk.zip")),
                    link("jmods", tag, release.resolve(prefix + "-jmods.zip")),
                    link("Maven jars", tag, release.resolve(prefix + "-maven.zip"))));
        }
    }
    notes.add("");
    notes.add("The Maven artifacts of version `%s` are attached as individual files, e.g. `javafx-graphics-<version>-linux.jar`."
            .formatted(mavenVersion));
    notes.add("");
    notes.add("### Configuration");
    notes.add("");
    notes.add("[configs/%s.md](%s/%s/blob/%s/configs/%s.md)".formatted(config.name(),
            System.getenv("GITHUB_SERVER_URL"), REPO, System.getenv("GITHUB_SHA"), config.name()));
    notes.add("");
    notes.add("| Setting | Value |");
    notes.add("|---------|-------|");
    notes.add("| Type | " + (config.snapshot() ? "snapshot" : "release") + " |");
    notes.add("| Build properties | " + code(config.buildProps()) + " |");
    notes.add("| Platforms | " + String.join(", ", config.platforms()) + " |");
    notes.add("| Boot JDK | " + config.bootJdkDistribution() + " " + config.bootJdkVersion() + " |");
    notes.add("| Tests | " + config.tests() + " |");
    notes.add("| Maven repository | " + (config.mavenRepository().isEmpty() ? "none" : config.mavenRepository()) + " |");
    notes.add("| Runners | " + code(config.runners().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toList()) + " |");
    notes.add("");
    notes.add(Files.readString(Path.of("merge-report.md")).strip());
    notes.add("");
    notes.add("### Tests");
    List<Path> tests;
    try (var list = Files.list(Path.of("tests"))) {
        tests = list.sorted().toList();
    }
    for (Path test : tests) {
        String platform = test.getFileName().toString().replaceFirst("^" + config.name() + "-tests-", "");
        notes.add("- " + platform + ": " + Files.readString(test.resolve("outcome.txt")).strip());
    }
    Files.writeString(Path.of("release-notes.md"), String.join("\n", notes) + "\n");

    // Tag the commit of the releases branch, if it exists, to keep the tags out of the history of master
    String target = ghSucceeds("api", "repos/" + REPO + "/branches/releases") ? "releases" : System.getenv("GITHUB_SHA");

    List<String> create = new ArrayList<>(List.of("release", "create", tag));
    files.forEach(f -> create.add(f.toString()));
    create.addAll(List.of("--title", "JavaFX " + tag, "--notes-file", "release-notes.md", "--target", target));
    gh(create.toArray(String[]::new));
    System.out.println("Published " + tag);
}
