package com.jairam.aicodeassistant.iam.adapter.persistence;

import com.jairam.aicodeassistant.iam.domain.model.Email;
import com.jairam.aicodeassistant.iam.domain.model.User;
import com.jairam.aicodeassistant.iam.domain.model.UserId;
import com.jairam.aicodeassistant.iam.domain.port.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** JPA-backed implementation of the {@link UserRepository} domain port. */
@Component
class JpaUserRepository implements UserRepository {

  private final UserJpaRepository jpa;

  JpaUserRepository(UserJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public User save(User user) {
    return IamPersistenceMapper.toDomain(jpa.save(IamPersistenceMapper.toEntity(user)));
  }

  @Override
  public Optional<User> findById(UserId id) {
    return jpa.findById(id.value()).map(IamPersistenceMapper::toDomain);
  }

  @Override
  public Optional<User> findByEmail(Email email) {
    return jpa.findByEmail(email.value()).map(IamPersistenceMapper::toDomain);
  }

  @Override
  public boolean existsByEmail(Email email) {
    return jpa.existsByEmail(email.value());
  }
}
