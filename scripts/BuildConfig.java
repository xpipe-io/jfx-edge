// Usage: java scripts/BuildConfig.java <configuration file> <checkout dir> >> "$GITHUB_OUTPUT"

String archs(Config config, String os) {
    return config.platforms().stream()
            .filter(platform -> platform.startsWith(os + "-"))
            .map(platform -> "\"" + platform.substring(os.length() + 1) + "\"")
            .collect(Collectors.joining(",", "[", "]"));
}

void main(String[] args) throws Exception {
    Config config = Config.read(Path.of(args[0]));
    Path dir = Path.of(args[1]);
    boolean compileWebkit = config.buildProps().contains("COMPILE_WEBKIT=true");
    boolean compileMedia = config.buildProps().contains("COMPILE_MEDIA=true");

    // HUDSON_JOB_NAME makes it a CI build, otherwise jfx would add a timestamp to the version
    // Who still uses hudson
    String buildTag = Shell.exec(dir, "git", "describe", "--tags", "--abbrev=0", "--match", "*+*");
    List<String> gradleProps = new ArrayList<>();
    gradleProps.add("-PHUDSON_JOB_NAME=jfx-openbuild");
    gradleProps.add("-PINCLUDE_APPS=false");
    if (compileMedia) {
        gradleProps.add("-PBUILD_LIBAV_STUBS=true");
    }
    gradleProps.add("-PRELEASE_SUFFIX=-" + config.name());
    gradleProps.add("-PPROMOTED_BUILD_NUMBER=" + buildTag.substring(buildTag.indexOf('+') + 1));
    config.buildProps().forEach(prop -> gradleProps.add("-P" + prop));

    String webTests = compileWebkit ? "" : " -x :web:test";
    String testArgs = switch (config.tests()) {
        case "skip" -> "";
        case "smoke" -> "test" + webTests;
        case "full" -> "-PFULL_TEST=true -PHEADLESS_TEST=true -PAWT_TEST=false test" + webTests;
        default -> throw new IllegalArgumentException("Unknown test mode " + config.tests() + ", supported are skip, smoke and full");
    };

    System.out.println("gradle_props=" + String.join(" ", gradleProps));
    System.out.println("test_args=" + testArgs);
    System.out.println("compile_webkit=" + compileWebkit);
    System.out.println("compile_media=" + compileMedia);
    System.out.println("maven_repository=" + config.mavenRepository());
    System.out.println("snapshot=" + config.snapshot());
    for (String os : List.of("linux", "macos", "windows")) {
        System.out.println(os + "_archs=" + archs(config, os));
    }
}
