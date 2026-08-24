package com.jairam.aicodeassistant.integration.github.internal;

import com.jairam.aicodeassistant.integration.config.GitHubProperties;
import com.jairam.aicodeassistant.integration.github.GitHubException;
import com.jairam.aicodeassistant.integration.github.GitHubRepo;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Low-level GitHub HTTP client. Owns the wire protocol: builds requests, maps HTTP status codes to
 * domain {@link GitHubException} subtypes, converts JSON DTOs to the public {@link GitHubRepo}, and
 * records per-call metrics + logs.
 *
 * <p>It performs NO retry/circuit-breaking itself — that policy is applied one layer up in {@code
 * ResilientGitHubGateway} so the retry/breaker semantics are declared in one place and this client
 * stays a thin, testable protocol adapter.
 *
 * <p>Failure mapping (see M3 doc "failure modes"):
 *
 * <ul>
 *   <li>401/403 → {@link GitHubException.CredentialRejected} (do NOT retry)
 *   <li>404 → thrown as CredentialRejected/Unavailable per context (not retried)
 *   <li>429 or 5xx → {@link GitHubException.Unavailable} (retryable)
 *   <li>timeout / connection error → {@link GitHubException.Unavailable} (retryable)
 * </ul>
 */
@Component
public class GitHubApiClient {

  private static final Logger log = LoggerFactory.getLogger(GitHubApiClient.class);

  private final RestClient apiClient;
  private final RestClient authClient;
  private final MeterRegistry metrics;

  GitHubApiClient(
      RestClient.Builder restClientBuilder, GitHubProperties properties, MeterRegistry metrics) {
    this.apiClient =
        restClientBuilder
            .clone()
            .baseUrl(properties.apiBaseUrl())
            .defaultHeader("Accept", "application/vnd.github+json")
            .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
            .build();
    this.authClient = restClientBuilder.clone().baseUrl(properties.authBaseUrl()).build();
    this.metrics = metrics;
  }

  /** Exchanges an OAuth code for an access token. Returns token + granted scopes. */
  TokenExchangeResult exchangeCodeForToken(
      String code, String clientId, String clientSecret, String redirectUri) {
    Timer.Sample sample = Timer.start(metrics);
    try {
      GitHubDtos.TokenResponse body =
          authClient
              .post()
              .uri("/access_token")
              .accept(org.springframework.http.MediaType.APPLICATION_JSON)
              .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
              .body(new TokenRequest(clientId, clientSecret, code, redirectUri))
              .retrieve()
              .onStatus(
                  HttpStatusCode::isError,
                  (req, res) -> mapError("oauth_token", res.getStatusCode()))
              .body(GitHubDtos.TokenResponse.class);
      record(sample, "oauth_token", "success");
      if (body == null || body.accessToken() == null || body.accessToken().isBlank()) {
        // GitHub returns 200 with an error field for bad codes.
        log.warn(
            "GitHub token exchange returned no token (error={})",
            body == null ? null : body.error());
        throw new GitHubException.CredentialRejected();
      }
      return new TokenExchangeResult(body.accessToken(), body.scope() == null ? "" : body.scope());
    } catch (GitHubException e) {
      record(sample, "oauth_token", "failure");
      throw e;
    } catch (RestClientException e) {
      record(sample, "oauth_token", "error");
      log.warn("GitHub token exchange transport error: {}", e.getMessage());
      throw new GitHubException.Unavailable(e.getMessage());
    }
  }

  /** Fetches the authenticated user's login + id for the given token. */
  AuthenticatedUser fetchAuthenticatedUser(String token) {
    return call(
        "get_user",
        () -> {
          GitHubDtos.UserResponse u =
              apiClient
                  .get()
                  .uri("/user")
                  .headers(h -> h.setBearerAuth(token))
                  .retrieve()
                  .onStatus(
                      HttpStatusCode::isError,
                      (req, res) -> mapError("get_user", res.getStatusCode()))
                  .body(GitHubDtos.UserResponse.class);
          if (u == null) {
            throw new GitHubException.Unavailable("empty /user response");
          }
          return new AuthenticatedUser(u.id(), u.login());
        });
  }

  /** Lists repos accessible to the token (first page; pagination is a later enhancement). */
  List<GitHubRepo> listRepositories(String token) {
    return call(
        "list_repos",
        () -> {
          GitHubDtos.RepoResponse[] repos =
              apiClient
                  .get()
                  .uri(uri -> uri.path("/user/repos").queryParam("per_page", "100").build())
                  .headers(h -> h.setBearerAuth(token))
                  .retrieve()
                  .onStatus(
                      HttpStatusCode::isError,
                      (req, res) -> mapError("list_repos", res.getStatusCode()))
                  .body(GitHubDtos.RepoResponse[].class);
          if (repos == null) {
            return List.of();
          }
          return java.util.Arrays.stream(repos).map(GitHubApiClient::toDomain).toList();
        });
  }

  /** Fetches one repository by owner/name. */
  GitHubRepo getRepository(String token, String owner, String name) {
    return call(
        "get_repo",
        () -> {
          GitHubDtos.RepoResponse r =
              apiClient
                  .get()
                  .uri("/repos/{owner}/{name}", owner, name)
                  .headers(h -> h.setBearerAuth(token))
                  .retrieve()
                  .onStatus(
                      HttpStatusCode::isError,
                      (req, res) -> mapError("get_repo", res.getStatusCode()))
                  .body(GitHubDtos.RepoResponse.class);
          if (r == null) {
            throw new GitHubException.Unavailable("empty /repos response");
          }
          return toDomain(r);
        });
  }

  private <T> T call(String operation, java.util.function.Supplier<T> action) {
    Timer.Sample sample = Timer.start(metrics);
    try {
      T result = action.get();
      record(sample, operation, "success");
      return result;
    } catch (GitHubException e) {
      record(sample, operation, "failure");
      throw e;
    } catch (RestClientException e) {
      // Transport-level failure (timeout, connection reset, DNS): retryable.
      record(sample, operation, "error");
      log.warn("GitHub {} transport error: {}", operation, e.getMessage());
      throw new GitHubException.Unavailable(e.getMessage());
    }
  }

  /** Translates an HTTP error status into a domain exception. Never leaks the body. */
  private static void mapError(String operation, HttpStatusCode status) {
    int code = status.value();
    if (code == 401 || code == 403) {
      // 403 with a rate-limit reason is possible; treat auth-forbidden as credential
      // rejection. Rate-limit 403/429 is handled as Unavailable below.
      if (code == 403) {
        throw new GitHubException.Unavailable("forbidden or rate-limited (403)");
      }
      throw new GitHubException.CredentialRejected();
    }
    if (code == 429 || status.is5xxServerError()) {
      throw new GitHubException.Unavailable("status " + code);
    }
    // 404 and other 4xx: not retryable, surface as unavailable-with-detail.
    throw new GitHubException.Unavailable("status " + code);
  }

  private void record(Timer.Sample sample, String operation, String outcome) {
    sample.stop(
        Timer.builder("github.api.call")
            .tag("operation", operation)
            .tag("outcome", outcome)
            .register(metrics));
  }

  private static GitHubRepo toDomain(GitHubDtos.RepoResponse r) {
    String owner = r.owner() == null ? "" : r.owner().login();
    return new GitHubRepo(
        Long.toString(r.id()),
        owner,
        r.name(),
        r.fullName(),
        r.cloneUrl(),
        r.defaultBranch() == null ? "main" : r.defaultBranch(),
        r.isPrivate());
  }

  /** OAuth token-exchange request body (form/JSON). Package-private. */
  record TokenRequest(
      @com.fasterxml.jackson.annotation.JsonProperty("client_id") String clientId,
      @com.fasterxml.jackson.annotation.JsonProperty("client_secret") String clientSecret,
      @com.fasterxml.jackson.annotation.JsonProperty("code") String code,
      @com.fasterxml.jackson.annotation.JsonProperty("redirect_uri") String redirectUri) {}

  /** Result of a token exchange: the raw token + granted scopes. Package-private. */
  record TokenExchangeResult(String accessToken, String scopes) {}

  /** The authenticated GitHub user. Package-private. */
  record AuthenticatedUser(long id, String login) {}
}
