package com.jairam.aicodeassistant.indexing.adapter.git;

import com.jairam.aicodeassistant.indexing.config.IndexingProperties;
import com.jairam.aicodeassistant.indexing.domain.RepositoryCloner;
import com.jairam.aicodeassistant.indexing.domain.SourceFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * JGit-based {@link RepositoryCloner}. Shallow-clones a single branch into a temporary directory
 * and reads its text files.
 *
 * <p>"Text file" filtering happens here (the cloner decides what the saga sees): files over {@link
 * IndexingProperties#maxFileBytes()}, or that appear binary (a NUL byte in the first bytes), or
 * under {@code .git}, are skipped. The clone directory is deleted on {@link
 * ClonedRepository#close()} — the saga always closes it (try-with-resources), so working copies
 * never leak.
 */
@Component
public class JGitRepositoryCloner implements RepositoryCloner {

  private static final Logger log = LoggerFactory.getLogger(JGitRepositoryCloner.class);
  private static final int BINARY_SNIFF_BYTES = 8000;

  private final long maxFileBytes;

  public JGitRepositoryCloner(IndexingProperties properties) {
    this.maxFileBytes = properties.maxFileBytes();
  }

  @Override
  public ClonedRepository clone(String cloneUrl, String branch) {
    Path workDir;
    try {
      workDir = Files.createTempDirectory("aca-index-");
    } catch (IOException e) {
      throw new CloneFailedException("could not create work dir", e);
    }
    try {
      // Shallow, single-branch clone keeps it fast and small.
      Git git =
          Git.cloneRepository()
              .setURI(cloneUrl)
              .setBranch(branch)
              .setBranchesToClone(List.of("refs/heads/" + branch))
              .setDepth(1)
              .setDirectory(workDir.toFile())
              .call();
      git.close();
      return new WorkTree(workDir, maxFileBytes);
    } catch (Exception e) {
      deleteQuietly(workDir);
      throw new CloneFailedException("git clone failed for " + cloneUrl, e);
    }
  }

  /** An open clone rooted at a temp directory. */
  static final class WorkTree implements ClonedRepository {
    private final Path root;
    private final long maxFileBytes;

    WorkTree(Path root, long maxFileBytes) {
      this.root = root;
      this.maxFileBytes = maxFileBytes;
    }

    @Override
    public List<SourceFile> textFiles() {
      List<SourceFile> files = new ArrayList<>();
      try (var stream = Files.walk(root)) {
        stream
            .filter(Files::isRegularFile)
            .filter(
                p ->
                    !p.toString()
                        .contains(java.io.File.separator + ".git" + java.io.File.separator))
            .forEach(
                p -> {
                  try {
                    if (Files.size(p) > maxFileBytes) {
                      return;
                    }
                    byte[] bytes = Files.readAllBytes(p);
                    if (looksBinary(bytes)) {
                      return;
                    }
                    String rel =
                        root.relativize(p).toString().replace(java.io.File.separatorChar, '/');
                    files.add(new SourceFile(rel, new String(bytes, StandardCharsets.UTF_8)));
                  } catch (IOException e) {
                    log.debug("Skipping unreadable file {}: {}", p, e.getMessage());
                  }
                });
      } catch (IOException e) {
        throw new CloneFailedException("failed to walk clone tree", e);
      }
      return files;
    }

    @Override
    public void close() {
      deleteQuietly(root);
    }
  }

  /** Heuristic binary sniff: a NUL byte in the first ~8KB. */
  static boolean looksBinary(byte[] bytes) {
    int limit = Math.min(bytes.length, BINARY_SNIFF_BYTES);
    for (int i = 0; i < limit; i++) {
      if (bytes[i] == 0) {
        return true;
      }
    }
    return false;
  }

  private static void deleteQuietly(Path dir) {
    if (dir == null) {
      return;
    }
    try (var stream = Files.walk(dir)) {
      stream.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
    } catch (IOException e) {
      LoggerFactory.getLogger(JGitRepositoryCloner.class)
          .warn("Failed to delete work dir {}: {}", dir, e.getMessage());
    }
  }
}
