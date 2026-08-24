package com.jairam.aicodeassistant.iam.application;

import com.jairam.aicodeassistant.iam.domain.model.Membership;
import com.jairam.aicodeassistant.iam.domain.model.User;
import com.jairam.aicodeassistant.iam.domain.model.UserId;
import com.jairam.aicodeassistant.iam.domain.port.MembershipRepository;
import com.jairam.aicodeassistant.iam.domain.port.UserRepository;
import com.jairam.aicodeassistant.platform.error.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-side use cases for the current user's profile and memberships.
 *
 * <p>Separated from the write services (CQRS-lite): queries are {@code readOnly} transactions and
 * return domain objects the REST layer maps to response DTOs.
 */
@Service
public class UserQueryService {

  private final UserRepository users;
  private final MembershipRepository memberships;

  public UserQueryService(UserRepository users, MembershipRepository memberships) {
    this.users = users;
    this.memberships = memberships;
  }

  /** Loads a user by id, or throws 404. */
  @Transactional(readOnly = true)
  public User getById(UUID userId) {
    return users
        .findById(new UserId(userId))
        .orElseThrow(() -> new ResourceNotFoundException("User", userId));
  }

  /** Lists the organizations (via memberships) the user belongs to. */
  @Transactional(readOnly = true)
  public List<Membership> membershipsOf(UUID userId) {
    return memberships.findByUser(new UserId(userId));
  }
}
