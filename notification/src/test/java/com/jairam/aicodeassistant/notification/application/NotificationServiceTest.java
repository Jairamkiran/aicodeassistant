package com.jairam.aicodeassistant.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jairam.aicodeassistant.notification.domain.Notification;
import com.jairam.aicodeassistant.notification.domain.NotificationStore;
import com.jairam.aicodeassistant.platform.error.ResourceNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link NotificationService} with in-memory fakes (no Spring, no DB). */
class NotificationServiceTest {

  private static final UUID USER = UUID.randomUUID();
  private static final UUID OTHER = UUID.randomUUID();
  private static final UUID ORG = UUID.randomUUID();

  private final InMemoryStore store = new InMemoryStore();
  private final RecordingDispatcher dispatcher = new RecordingDispatcher();
  private final NotificationService service = new NotificationService(store, dispatcher);

  private Notification sample(UUID recipient) {
    return Notification.create(
        recipient, ORG, "REPOSITORY_INDEXED", "Ready", "done", "REPOSITORY", "r1", Instant.EPOCH);
  }

  @Test
  void createPersistsAndDispatches() {
    Notification saved = service.create(sample(USER));

    assertThat(store.findById(saved.id())).isPresent();
    assertThat(dispatcher.dispatched).hasSize(1);
    assertThat(saved.isRead()).isFalse();
  }

  @Test
  void unreadCountReflectsUnreadOnly() {
    service.create(sample(USER));
    service.create(sample(USER));
    assertThat(service.unreadCount(USER)).isEqualTo(2);

    List<Notification> list = service.list(USER, 10);
    service.markRead(USER, list.get(0).id());

    assertThat(service.unreadCount(USER)).isEqualTo(1);
  }

  @Test
  void markReadRejectsAnotherUsersNotification() {
    Notification others = service.create(sample(OTHER));

    assertThatThrownBy(() -> service.markRead(USER, others.id()))
        .isInstanceOf(ResourceNotFoundException.class);
    // Untouched.
    assertThat(store.findById(others.id()).orElseThrow().isRead()).isFalse();
  }

  @Test
  void listIsScopedToRecipient() {
    service.create(sample(USER));
    service.create(sample(OTHER));

    assertThat(service.list(USER, 10)).hasSize(1);
  }

  // --- fakes -------------------------------------------------------------------

  private static final class InMemoryStore implements NotificationStore {
    private final List<Notification> data = new ArrayList<>();

    @Override
    public Notification save(Notification notification) {
      data.removeIf(n -> n.id().equals(notification.id()));
      data.add(notification);
      return notification;
    }

    @Override
    public Optional<Notification> findById(UUID id) {
      return data.stream().filter(n -> n.id().equals(id)).findFirst();
    }

    @Override
    public List<Notification> findByRecipient(UUID recipientUserId, int limit) {
      return data.stream()
          .filter(n -> n.recipientUserId().equals(recipientUserId))
          .sorted(Comparator.comparing(Notification::createdAt).reversed())
          .limit(limit)
          .toList();
    }

    @Override
    public long countUnread(UUID recipientUserId) {
      return data.stream()
          .filter(n -> n.recipientUserId().equals(recipientUserId) && !n.isRead())
          .count();
    }
  }

  private static final class RecordingDispatcher implements NotificationDispatcher {
    private final List<Notification> dispatched = new ArrayList<>();

    @Override
    public void dispatch(Notification notification) {
      dispatched.add(notification);
    }
  }
}
