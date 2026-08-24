package com.jairam.aicodeassistant.iam.adapter.rest;

import com.jairam.aicodeassistant.iam.adapter.rest.dto.AddMemberRequest;
import com.jairam.aicodeassistant.iam.adapter.rest.dto.CreateOrganizationRequest;
import com.jairam.aicodeassistant.iam.adapter.rest.dto.CreateOrganizationResponse;
import com.jairam.aicodeassistant.iam.application.OrganizationService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Organization endpoints.
 *
 * <ul>
 *   <li>{@code POST /api/v1/organizations} — any authenticated user creates an org and becomes its
 *       OWNER.
 *   <li>{@code POST /api/v1/organizations/{id}/members} — add/re-role a member; the caller must
 *       hold at least ADMIN in that org (enforced in the application service via membership
 *       lookup).
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/organizations")
class OrganizationController {

  private final OrganizationService organizationService;

  OrganizationController(OrganizationService organizationService) {
    this.organizationService = organizationService;
  }

  @PostMapping
  ResponseEntity<CreateOrganizationResponse> create(
      Authentication authentication, @Valid @RequestBody CreateOrganizationRequest request) {
    UUID actingUser = CurrentUser.id(authentication);
    var result = organizationService.create(actingUser, request.name());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new CreateOrganizationResponse(result.organizationId(), result.slug()));
  }

  @PostMapping("/{organizationId}/members")
  ResponseEntity<Void> addMember(
      Authentication authentication,
      @PathVariable String organizationId,
      @Valid @RequestBody AddMemberRequest request) {
    UUID actingUser = CurrentUser.id(authentication);
    organizationService.addMember(
        actingUser, organizationId, request.userId().toString(), request.role());
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
