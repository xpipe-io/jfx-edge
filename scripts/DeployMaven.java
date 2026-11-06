// Usage: java scripts/DeployMaven.java <configuration file> <output directory of PrepareMaven>
// The credentials come from the environment variables MAVEN_USERNAME and MAVEN_PASSWORD.
// If GPG_KEY is set, the files are also signed with the imported key GPG_KEY_ID and the passphrase GPG_KEY_PASSWORD.

final Path HERE = Path.of(".");

String classifier(Path jar, String prefix) {
    String name = jar.getFileName().toString();
    return name.substring(prefix.length(), name.length() - ".jar".length());
}

void main(String[] args) throws Exception {
    Config config = Config.read(Path.of(args[0]));
    Path dir = Path.of(args[1]);

    Path settings = Files.writeString(Files.createTempFile("settings", ".xml"), """
            <settings>
              <servers>
                <server>
                  <id>maven-repository</id>
                  <username>${env.MAVEN_USERNAME}</username>
                  <password>${env.MAVEN_PASSWORD}</password>
                </server>
              </servers>
            </settings>
            """);

    boolean sign = System.getenv("GPG_KEY") != null && !System.getenv("GPG_KEY").isEmpty();
    List<String> goal = sign
            ? List.of("org.apache.maven.plugins:maven-gpg-plugin:3.2.8:sign-and-deploy-file",
                    "-Dgpg.keyname=" + System.getenv("GPG_KEY_ID"), "-Dgpg.passphraseEnvName=GPG_KEY_PASSWORD")
            : List.of("org.apache.maven.plugins:maven-deploy-plugin:3.1.4:deploy-file");

    List<Path> poms;
    try (var files = Files.list(dir)) {
        poms = files.filter(f -> f.getFileName().toString().endsWith(".pom")).sorted().toList();
    }
    for (Path pom : poms) {
        // e.g. javafx-graphics-28-nightly+14-SNAPSHOT
        String prefix = pom.getFileName().toString().replaceFirst("\\.pom$", "");
        Path mainJar = dir.resolve(prefix + ".jar");
        List<String> cmd = new ArrayList<>(List.of("mvn", "-B", "-s", settings.toString()));
        cmd.addAll(goal);
        cmd.addAll(List.of("-Durl=" + config.mavenRepository(), "-DrepositoryId=maven-repository", "-DpomFile=" + pom,
                "-Dfile=" + (Files.exists(mainJar) ? mainJar : pom)));

        List<Path> classifiedJars;
        try (var files = Files.list(dir)) {
            classifiedJars = files.filter(f -> f.getFileName().toString().startsWith(prefix + "-")
                    && f.getFileName().toString().endsWith(".jar")).sorted().toList();
        }
        List<String> classifiers = classifiedJars.stream().map(jar -> classifier(jar, prefix + "-")).toList();
        if (!classifiedJars.isEmpty()) {
            cmd.add("-Dfiles=" + classifiedJars.stream().map(Path::toString).collect(Collectors.joining(",")));
            cmd.add("-Dclassifiers=" + String.join(",", classifiers));
            cmd.add("-Dtypes=" + classifiedJars.stream().map(jar -> "jar").collect(Collectors.joining(",")));
        }

        System.out.println("Deploying " + prefix + (classifiers.isEmpty() ? "" : " with " + String.join(", ", classifiers)));
        Shell.exec(HERE, cmd.toArray(String[]::new)).lines()
                .filter(line -> line.contains("Uploaded to "))
                .forEach(line -> System.out.println("  " + line.replace("[INFO] ", "")));
    }
}
