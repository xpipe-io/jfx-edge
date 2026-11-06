// Usage: java scripts/Package.java <platform>, e.g. linux-aarch64

Path find(Path dir, String prefix) throws IOException {
    try (var files = Files.list(dir)) {
        return files.filter(f -> f.getFileName().toString().startsWith(prefix)).findFirst().orElseThrow();
    }
}

void main(String[] args) throws Exception {
    String platform = args[0];
    String os = platform.substring(0, platform.indexOf('-'));
    String arch = platform.substring(platform.indexOf('-') + 1);

    String bundlePlatform = platform.replace("macos", "osx");
    String classifier = Map.of("linux", "linux", "macos", "mac", "windows", "win").get(os);
    String archSuffix = arch.equals("x64") ? "" : "-" + arch;

    Path jfx = Path.of("jfx");
    Properties properties = new Properties();
    try (var reader = Files.newBufferedReader(jfx.resolve("modules/javafx.base/build/module-lib/javafx.properties"))) {
        properties.load(reader);
    }
    String version = properties.getProperty("javafx.runtime.version");
    String name = "openjfx-" + version + "_" + bundlePlatform + "_bin";

    Path release = Files.createDirectories(Path.of("release"));
    Path bundles = jfx.resolve("build/artifacts/bundles");
    Files.copy(find(bundles, "javafx-sdk-"), release.resolve(name + "-sdk.zip"));
    Files.copy(find(bundles, "javafx-jmods-"), release.resolve(name + "-jmods.zip"));
    Files.copy(find(bundles, "javafx-docs-"), release.resolve("openjfx-" + version + "-javadoc.zip"));

    String platformJar = "-" + classifier + ".jar";
    String renamedPlatformJar = "-" + classifier + archSuffix + ".jar";

    List<Path> jars;
    try (var files = Files.list(jfx.resolve("build/publications"))) {
        jars = files.filter(f -> f.getFileName().toString().endsWith(platformJar)).sorted().toList();
    }
    try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(release.resolve(name + "-maven.zip")))) {
        for (Path jar : jars) {
            zip.putNextEntry(new ZipEntry(jar.getFileName().toString().replace(platformJar, renamedPlatformJar)));
            Files.copy(jar, zip);
            zip.closeEntry();
        }
    }

    Path mavenRepo = jfx.resolve("maven-repo");
    List<Path> repoFiles;
    try (var files = Files.walk(mavenRepo)) {
        repoFiles = files.filter(Files::isRegularFile).toList();
    }
    for (Path file : repoFiles) {
        Path target = Path.of("maven-repo").resolve(mavenRepo.relativize(file).toString().replace(platformJar, renamedPlatformJar));
        Files.createDirectories(target.getParent());
        Files.copy(file, target);
    }
}
