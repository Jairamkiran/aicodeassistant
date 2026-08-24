package com.jairam.aicodeassistant.repository.adapter.rest.dto;

/** API view of a linkable GitHub repository (for the import picker). */
public record GitHubRepoView(
    String externalId,
    String owner,
    String name,
    String fullName,
    String defaultBranch,
    boolean isPrivate) {}
