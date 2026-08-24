package com.jairam.aicodeassistant.iam.adapter.rest;

import com.jairam.aicodeassistant.iam.adapter.rest.dto.CurrentUserResponse;
import com.jairam.aicodeassistant.iam.application.UserQueryService;
import com.jairam.aicodeassistant.iam.domain.model.User;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User endpoints. {@code GET /api/v1/users/me} returns the authenticated user's profile and
 * memberships. Requires a valid bearer token (enforced by the security filter chain — any
 * authenticated principal may read its own profile).
 */
@RestController
@RequestMapping("/api/v1/users")
class UserController {

  private final UserQueryService userQueryService;

  UserController(UserQueryService userQueryService) {
    this.userQueryService = userQueryService;
  }

  @GetMapping("/me")
  CurrentUserResponse me(Authentication authentication) {
    UUID userId = CurrentUser.id(authentication);
    User user = userQueryService.getById(userId);
    List<CurrentUserResponse.MembershipView> memberships =
        userQueryService.membershipsOf(userId).stream()
            .map(
                m ->
                    new CurrentUserResponse.MembershipView(
                        m.organizationId().value(), m.role().name()))
            .toList();
    return new CurrentUserResponse(
        user.id().value(),
        user.email().value(),
        user.displayName(),
        user.status().name(),
        memberships);
  }
}
