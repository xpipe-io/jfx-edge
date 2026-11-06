// Usage: java scripts/PrepareMaven.java <configuration file> <merged local Maven repository> <release bundles> <output directory>
// Writes all Maven files with the version they are published with, e.g. javafx-graphics-28-nightly+14-SNAPSHOT-linux.jar

void copyDirectory(Path zipFile, String dir, Path jar) throws IOException {
    try (ZipFile zip = new ZipFile(zipFile.toFile()); ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
        for (ZipEntry entry : Collections.list(zip.entries())) {
            if (entry.isDirectory() || !entry.getName().startsWith(dir)) {
                continue;
            }
            out.putNextEntry(new ZipEntry(entry.getName().substring(dir.length())));
            try (InputStream in = zip.getInputStream(entry)) {
                in.transferTo(out);
            }
            out.closeEntry();
        }
    }
}

Path findBundle(Path bundles, String suffix) throws IOException {
    try (var files = Files.list(bundles)) {
        return files.filter(f -> f.getFileName().toString().endsWith(suffix)).findFirst().orElseThrow();
    }
}

void main(String[] args) throws Exception {
    Config config = Config.read(Path.of(args[0]));
    Path repo = Path.of(args[1], "org", "openjfx");
    Path bundles = Path.of(args[2]);
    Path out = Files.createDirectories(Path.of(args[3]));

    Path sources = Files.createTempFile("src", ".zip");
    try (ZipFile sdk = new ZipFile(findBundle(bundles, "_bin-sdk.zip").toFile())) {
        ZipEntry entry = sdk.stream().filter(e -> e.getName().endsWith("/src.zip")).findFirst().orElseThrow();
        try (InputStream in = sdk.getInputStream(entry)) {
            Files.copy(in, sources, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    Path javadoc = findBundle(bundles, "-javadoc.zip");
    String javadocDir;
    try (ZipFile zip = new ZipFile(javadoc.toFile())) {
        javadocDir = zip.stream().map(ZipEntry::getName).filter(name -> name.endsWith("/index.html"))
                .min(Comparator.comparingInt(String::length)).orElseThrow().replace("index.html", "");
    }

    String version;
    try (var dirs = Files.list(repo.resolve("javafx"))) {
        version = dirs.filter(Files::isDirectory).findFirst().orElseThrow().getFileName().toString();
    }
    String deployVersion = config.snapshot() ? version + "-SNAPSHOT" : version;

    Files.writeString(repo.resolve("javafx", version, "javafx-" + version + ".pom"),
            Files.readString(Path.of("maven", "javafx.pom")).replace("@VERSION@", version));

    List<Path> artifactDirs;
    try (var dirs = Files.list(repo)) {
        artifactDirs = dirs.filter(Files::isDirectory).sorted().toList();
    }
    for (Path artifactDir : artifactDirs) {
        String artifact = artifactDir.getFileName().toString();
        Path dir = artifactDir.resolve(version);
        String prefix = artifact + "-" + version;
        String deployPrefix = artifact + "-" + deployVersion;

        String pom = Files.readString(dir.resolve(prefix + ".pom"))
                .replace("<version>" + version + "</version>", "<version>" + deployVersion + "</version>");
        if (!pom.contains("<name>")) {
            String ownVersion = "<version>" + deployVersion + "</version>";
            pom = pom.replaceFirst(Pattern.quote(ownVersion), Matcher.quoteReplacement(ownVersion
                    + "\n  <name>%s</name>".formatted(artifact.replace('-', ' '))
                    + "\n  <description>OpenJFX %s</description>".formatted(artifact)
                    + "\n  <url>https://openjdk.java.net/projects/openjfx/</url>"));
        }
        Files.writeString(out.resolve(deployPrefix + ".pom"), pom);

        try (var files = Files.list(dir)) {
            for (Path jar : files.filter(f -> f.getFileName().toString().endsWith(".jar")).toList()) {
                Files.copy(jar, out.resolve(jar.getFileName().toString().replace(prefix, deployPrefix)));
            }
        }
        if (Files.exists(dir.resolve(prefix + ".jar"))) {
            copyDirectory(sources, artifact.replace('-', '.') + "/", out.resolve(deployPrefix + "-sources.jar"));
            copyDirectory(javadoc, javadocDir, out.resolve(deployPrefix + "-javadoc.jar"));
        }
    }
}
