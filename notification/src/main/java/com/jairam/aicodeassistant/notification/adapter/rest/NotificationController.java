package com.jairam.aicodeassistant.notification.adapter.rest;

import com.jairam.aicodeassistant.notification.adapter.rest.dto.NotificationView;
import com.jairam.aicodeassistant.notification.application.NotificationService;
import com.jairam.aicodeassistant.platform.error.ApplicationException;
import com.jairam.aicodeassistant.platform.error.ErrorType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * In-app notification endpoints for the current user.
 *
 * <ul>
 *   <li>{@code GET /api/v1/notifications?limit=} — recent notifications, newest first.
 *   <li>{@code GET /api/v1/notifications/unread-count} — number of unread notifications.
 *   <li>{@code POST /api/v1/notifications/{id}/read} — mark one read.
 * </ul>
 *
 * <p>Every route is scoped to the authenticated user; a user can only see and mutate their own
 * notifications.
 */
@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController {

  private final NotificationService service;

  NotificationController(NotificationService service) {
    this.service = service;
  }

  @GetMapping
  List<NotificationView> list(
      Authentication authentication, @RequestParam(name = "limit", defaultValue = "20") int limit) {
    UUID userId = currentUserId(authentication);
    return service.list(userId, limit).stream().map(NotificationView::from).toList();
  }

  @GetMapping("/unread-count")
  UnreadCountView unreadCount(Authentication authentication) {
    UUID userId = currentUserId(authentication);
    return new UnreadCountView(service.unreadCount(userId));
  }

  @PostMapping("/{id}/read")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void markRead(Authentication authentication, @PathVariable UUID id) {
    UUID userId = currentUserId(authentication);
    service.markRead(userId, id);
  }

  private static UUID currentUserId(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new NotAuthenticatedException();
    }
    return UUID.fromString(authentication.getName());
  }

  /** Unread-count response body. */
  record UnreadCountView(long unread) {}

  /** No authenticated principal. HTTP 401. */
  static final class NotAuthenticatedException extends ApplicationException {
    private static final long serialVersionUID = 1L;

    NotAuthenticatedException() {
      super(ErrorType.AUTHENTICATION, HttpStatus.UNAUTHORIZED, "Not authenticated", Map.of());
    }
  }
}
