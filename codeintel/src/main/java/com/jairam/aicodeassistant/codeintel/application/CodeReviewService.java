package com.jairam.aicodeassistant.codeintel.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jairam.aicodeassistant.ai.chat.ChatModel;
import com.jairam.aicodeassistant.ai.chat.ChatRequest;
import com.jairam.aicodeassistant.ai.chat.ChatResponse;
import com.jairam.aicodeassistant.ai.chat.StructuredOutputException;
import com.jairam.aicodeassistant.ai.chat.StructuredOutputs;
import com.jairam.aicodeassistant.codeintel.domain.CodeReview;
import com.jairam.aicodeassistant.codeintel.domain.ReviewFinding;
import com.jairam.aicodeassistant.codeintel.domain.ReviewSeverity;
import com.jairam.aicodeassistant.retrieval.search.CodeSearch;
import com.jairam.aicodeassistant.retrieval.search.SearchQuery;
import com.jairam.aicodeassistant.retrieval.search.SearchResult;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Produces a structured {@link CodeReview} for a repository and focus topic: retrieve relevant code
 * ({@code retrieval :: search}), prompt the model for a JSON review ({@code ai :: chat}), then
 * parse the structured output ({@link StructuredOutputs}) into domain findings.
 *
 * <p>This is the first real consumer of structured AI output (M9). The model's JSON is a transport
 * detail — the private {@link ReviewJson} DTO maps it to domain {@link ReviewFinding}s so no
 * provider/JSON shape leaks out of this module.
 */
@Service
public class CodeReviewService {

  private static final Logger log = LoggerFactory.getLogger(CodeReviewService.class);

  private final CodeSearch codeSearch;
  private final ChatModel chatModel;
  private final ObjectMapper mapper;
  private final CodeIntelProperties properties;

  public CodeReviewService(
      CodeSearch codeSearch,
      ChatModel chatModel,
      ObjectMapper mapper,
      CodeIntelProperties properties) {
    this.codeSearch = codeSearch;
    this.chatModel = chatModel;
    this.mapper = mapper;
    this.properties = properties;
  }

  /**
   * Reviews {@code focus} over the given repository within an organization.
   *
   * @throws StructuredOutputException if the model reply is not parseable structured output
   */
  public CodeReview review(UUID organizationId, UUID repositoryId, String focus) {
    List<SearchResult> retrieved =
        codeSearch.search(
            new SearchQuery(
                organizationId, repositoryId, focus, properties.reviewRetrievalLimit()));

    List<com.jairam.aicodeassistant.ai.chat.ChatMessage> messages =
        new ReviewPromptBuilder(properties.contextCharBudget()).build(focus, retrieved);

    ChatResponse response = chatModel.chat(ChatRequest.of(messages));
    ReviewJson parsed = StructuredOutputs.parse(response.content(), ReviewJson.class, mapper);

    List<ReviewFinding> findings =
        (parsed.findings() == null ? List.<ReviewJson.FindingJson>of() : parsed.findings())
            .stream().map(CodeReviewService::toFinding).toList();

    log.info(
        "Code review for repository {} (focus='{}') produced {} findings",
        repositoryId,
        focus,
        findings.size());
    return CodeReview.of(repositoryId, focus, parsed.summary(), findings);
  }

  private static ReviewFinding toFinding(ReviewJson.FindingJson f) {
    return new ReviewFinding(
        ReviewSeverity.fromString(f.severity()),
        f.category(),
        f.filePath(),
        Math.max(0, f.startLine()),
        Math.max(0, f.endLine()),
        f.title(),
        f.detail(),
        f.recommendation());
  }

  // --- structured-output DTO (private; the model's JSON shape never leaks) -----
  @JsonIgnoreProperties(ignoreUnknown = true)
  record ReviewJson(String summary, List<FindingJson> findings) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FindingJson(
        String severity,
        String category,
        String filePath,
        int startLine,
        int endLine,
        String title,
        String detail,
        String recommendation) {}
  }
}
