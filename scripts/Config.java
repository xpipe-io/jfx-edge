import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

record Config(String name, boolean snapshot, Link base, List<Link> mergeList, List<String> buildProps, List<String> platforms,
              String bootJdkDistribution, String bootJdkVersion, String tests, String mavenRepository, Map<String, String> runners) {

    static final List<String> SUPPORTED_PLATFORMS =
            List.of("linux-x64", "linux-aarch64", "macos-x64", "macos-aarch64", "windows-x64");

    record Link(String owner, String repo, String kind, String ref) {

        String url() {
            return "https://github.com/%s/%s/%s/%s".formatted(owner, repo, kind, ref);
        }

        String repoUrl() {
            return "https://github.com/%s/%s.git".formatted(owner, repo);
        }

        String fetchRef() {
            return kind.equals("pull") ? "refs/pull/" + ref + "/head" : ref;
        }
    }

    private static final Pattern LINK =
            Pattern.compile("https://github\\.com/([^/\\s]+)/([^/\\s]+)/(pull|tree|commit)/(\\S+)");

    static Config read(Path file) throws IOException {
        String text = Files.readString(file);

        String name = file.getFileName().toString().replaceFirst("\\.md$", "");
        if (!name.matches("[A-Za-z0-9]+")) {
            throw new IllegalArgumentException("Invalid configuration name " + name + ", only letters and digits are allowed");
        }

        List<String> platforms = items(text, "PLATFORMS");
        for (String platform : platforms) {
            if (!SUPPORTED_PLATFORMS.contains(platform)) {
                throw new IllegalArgumentException("Unknown platform " + platform + ", supported are " + SUPPORTED_PLATFORMS);
            }
        }

        Map<String, String> bootJdk = keyValues(text, "BOOT-JDK");

        boolean snapshot = switch (items(text, "TYPE").getFirst()) {
            case "snapshot" -> true;
            case "release" -> false;
            default -> throw new IllegalArgumentException("Unknown type " + items(text, "TYPE").getFirst() + ", supported are snapshot and release");
        };

        return new Config(
                name,
                snapshot,
                links(text, "BASE").getFirst(),
                links(text, "MERGE-LIST"),
                items(text, "BUILD-PROPS"),
                platforms,
                bootJdk.get("distribution"),
                bootJdk.get("version"),
                items(text, "TESTS").getFirst(),
                String.join("", items(text, "MAVEN-REPOSITORY")),
                keyValues(text, "RUNNERS"));
    }

    private static String block(String text, String name) {
        Matcher m = Pattern.compile("<!-- " + name + "-START -->(.*?)<!-- " + name + "-END -->", Pattern.DOTALL)
                .matcher(text);
        if (!m.find()) {
            throw new IllegalArgumentException("The configuration has no " + name + " block");
        }
        return m.group(1);
    }

    private static List<String> items(String text, String name) {
        return block(text, name).lines()
                .map(String::strip)
                .map(line -> line.startsWith("- ") ? line.substring(2).strip() : line)
                .filter(line -> !line.isEmpty())
                .toList();
    }

    private static Map<String, String> keyValues(String text, String name) {
        return items(text, name).stream()
                .map(item -> item.split("=", 2))
                .collect(Collectors.toMap(kv -> kv[0].strip(), kv -> kv[1].strip(), (a, b) -> b, LinkedHashMap::new));
    }

    private static List<Link> links(String text, String name) {
        return LINK.matcher(block(text, name)).results()
                .map(m -> new Link(m.group(1), m.group(2), m.group(3), m.group(4).replaceAll("/$", "")))
                .toList();
    }
}
