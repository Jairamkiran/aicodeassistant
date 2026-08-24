package com.jairam.aicodeassistant.integration.github;

/**
 * A GitHub repository, expressed as a neutral domain record that other modules consume. This is the
 * module's public representation — GitHub's JSON DTOs stay private to the adapter and are converted
 * to this before crossing the boundary.
 *
 * @param externalId GitHub's numeric repo id, as a string (stable across renames)
 * @param owner repository owner login
 * @param name repository name
 * @param fullName {@code owner/name}
 * @param cloneUrl HTTPS clone URL
 * @param defaultBranch default branch name
 * @param isPrivate whether the repo is private
 */
public record GitHubRepo(
    String externalId,
    String owner,
    String name,
    String fullName,
    String cloneUrl,
    String defaultBranch,
    boolean isPrivate) {}
