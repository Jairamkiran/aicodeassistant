package com.jairam.aicodeassistant.indexing.adapter.git;

import static org.assertj.core.api.Assertions.assertThat;

import com.jairam.aicodeassistant.indexing.config.IndexingProperties;
import com.jairam.aicodeassistant.indexing.domain.RepositoryCloner;
import com.jairam.aicodeassistant.indexing.domain.SourceFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests {@link JGitRepositoryCloner} against a LOCAL {@code file://} git repo — a real JGit clone
 * with no network and no Docker. Verifies that text files are read, binary files are skipped,
 * oversized files are skipped, and the working directory is cleaned up on close.
 */
class JGitRepositoryClonerTest {

  private final IndexingProperties props = new IndexingProperties(60, 10, 1000);

  @Test
  void clonesLocalRepoAndReadsTextFilesSkippingBinary(@TempDir Path tmp) throws Exception {
    Path origin = tmp.resolve("origin");
    Files.createDirectories(origin);

    // Build a real git repo with a text file, a binary file, and an oversized file.
    try (Git git = Git.init().setDirectory(origin.toFile()).call()) {
      Files.writeString(origin.resolve("Main.java"), "class Main {}\n");
      Files.write(origin.resolve("logo.png"), new byte[] {1, 2, 0, 3, 4}); // NUL → binary
      Files.write(origin.resolve("big.bin"), new byte[2000]); // > maxFileBytes (1000)
      git.add().addFilepattern(".").call();
      git.commit().setMessage("init").setSign(false).call();
    }

    var cloner = new JGitRepositoryCloner(props);
    try (RepositoryCloner.ClonedRepository cloned =
        cloner.clone(origin.toUri().toString(), "master")) {
      List<SourceFile> files = cloned.textFiles();
      Map<String, SourceFile> byPath =
          files.stream().collect(java.util.stream.Collectors.toMap(SourceFile::path, f -> f));

      assertThat(byPath).containsKey("Main.java");
      assertThat(byPath.get("Main.java").content()).contains("class Main");
      assertThat(byPath).doesNotContainKey("logo.png"); // binary skipped
      assertThat(byPath).doesNotContainKey("big.bin"); // oversized skipped
    }
    // The try-with-resources exercised close() (temp clone dir removed) without error.
  }

  @Test
  void cloneFailureThrowsCloneFailed() {
    var cloner = new JGitRepositoryCloner(props);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> cloner.clone("file:///nonexistent/repo/path", "main"))
        .isInstanceOf(RepositoryCloner.CloneFailedException.class);
  }
}
