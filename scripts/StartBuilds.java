// Usage: java scripts/StartBuilds.java <force>

String quote(String value) {
    return "\"" + value + "\"";
}

// e.g. {"default":"ubuntu-26.04","linux-aarch64":["self-hosted","arm64"]}
String json(Map<String, String> runners) {
    List<String> entries = new ArrayList<>();
    for (var runner : runners.entrySet()) {
        List<String> labels = Arrays.stream(runner.getValue().split(",")).map(String::strip).map(this::quote).toList();
        String value = labels.size() == 1 ? labels.getFirst() : "[" + String.join(",", labels) + "]";
        entries.add(quote(runner.getKey()) + ":" + value);
    }
    return "{" + String.join(",", entries) + "}";
}

void main(String[] args) throws Exception {
    List<Path> files;
    try (var list = Files.list(Path.of("configs"))) {
        files = list.filter(f -> f.toString().endsWith(".md")).sorted().toList();
    }
    for (Path file : files) {
        Config config = Config.read(file);
        Shell.exec(Path.of("."), "gh", "workflow", "run", "build-config.yml", "--ref", System.getenv("GITHUB_REF_NAME"),
                "-f", "config=" + config.name(),
                "-f", "runners=" + json(config.runners()),
                "-f", "boot_jdk_distribution=" + config.bootJdkDistribution(),
                "-f", "boot_jdk_version=" + config.bootJdkVersion(),
                "-f", "force=" + args[0]);
        System.out.println("Started the build of " + config.name());
    }
}
