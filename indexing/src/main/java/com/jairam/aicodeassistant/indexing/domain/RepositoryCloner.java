package com.jairam.aicodeassistant.indexing.domain;

import java.util.List;

/**
 * Outbound port: clones a repository and yields its text files.
 *
 * <p>The returned {@link ClonedRepository} is an {@link AutoCloseable} handle so the saga can clean
 * up the working directory in a try-with-resources / finally, even on failure. Binary and oversized
 * files are skipped by the implementation.
 */
public interface RepositoryCloner {

  /**
   * Clones {@code cloneUrl} at {@code branch} into a temporary working directory.
   *
   * @throws CloneFailedException if the clone cannot be performed
   */
  ClonedRepository clone(String cloneUrl, String branch);

  /** An open clone; {@link #close()} deletes the working directory. */
  interface ClonedRepository extends AutoCloseable {

    /** The repository's text files (binaries/oversized files already excluded). */
    List<SourceFile> textFiles();

    @Override
    void close();
  }

  /** Raised when cloning fails (bad URL, auth, network, disk). */
  class CloneFailedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CloneFailedException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
