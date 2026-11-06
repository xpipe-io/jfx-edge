// Usage: java scripts/MergeArtifacts.java <directory with one subdirectory per artifact> <output directory>
// Files contained in several artifacts, like the javadoc and the shared Maven files, are copied from the first one

void main(String[] args) throws Exception {
    Path artifacts = Path.of(args[0]);
    Path out = Path.of(args[1]);

    List<Path> dirs;
    try (var list = Files.list(artifacts)) {
        dirs = list.sorted().toList();
    }
    for (Path dir : dirs) {
        List<Path> files;
        try (var walk = Files.walk(dir)) {
            files = walk.filter(Files::isRegularFile).toList();
        }
        for (Path file : files) {
            Path target = out.resolve(dir.relativize(file).toString());
            if (Files.notExists(target)) {
                Files.createDirectories(target.getParent());
                Files.copy(file, target);
            }
        }
    }
}
