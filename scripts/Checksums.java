// Usage: java scripts/Checksums.java <directory>
// Writes an .md5 and .sha1 file for every file in the directory, like Maven Central has them

void main(String[] args) throws Exception {
    List<Path> files;
    try (var list = Files.list(Path.of(args[0]))) {
        files = list.filter(Files::isRegularFile).sorted().toList();
    }
    for (Path file : files) {
        byte[] content = Files.readAllBytes(file);
        for (String algorithm : List.of("MD5", "SHA-1")) {
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(content));
            String extension = algorithm.toLowerCase(Locale.ROOT).replace("-", "");
            Files.writeString(file.resolveSibling(file.getFileName() + "." + extension), hash);
        }
    }
}
