import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

final class Shell {

    record Result(int exitCode, String stdout, String stderr) {}

    static Result run(Path dir, String... cmd) throws IOException, InterruptedException {
        System.err.println("+ " + String.join(" ", cmd));
        Process process = new ProcessBuilder(cmd).directory(dir.toFile()).start();
        process.getOutputStream().close();
        // Read stderr in parallel, otherwise the process can block on a full pipe
        CompletableFuture<String> stderr = CompletableFuture.supplyAsync(() -> read(process.getErrorStream()));
        String stdout = read(process.getInputStream());
        return new Result(process.waitFor(), stdout, stderr.join());
    }

    static String exec(Path dir, String... cmd) throws IOException, InterruptedException {
        Result result = run(dir, cmd);
        if (result.exitCode() != 0) {
            throw new IllegalStateException(String.join(" ", cmd) + " failed: " + result.stderr().strip());
        }
        return result.stdout().strip();
    }

    private static String read(InputStream in) {
        try (in) {
            return new String(in.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
