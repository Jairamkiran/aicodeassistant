package com.jairam.aicodeassistant.audit.application;

import com.jairam.aicodeassistant.audit.AuditRecorder;
import com.jairam.aicodeassistant.audit.domain.ActorType;
import com.jairam.aicodeassistant.audit.domain.AuditEvent;
import com.jairam.aicodeassistant.audit.domain.AuditOutcome;
import com.jairam.aicodeassistant.platform.audit.AuditSignal;
import com.jairam.aicodeassistant.platform.observability.CorrelationId;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Bridges neutral {@link AuditSignal}s published anywhere in the system into the audit log. This is
 * the audit module's only inbound coupling — to the shared kernel's signal type, never to another
 * context's internals.
 *
 * <p>Runs synchronously in the caller's thread but records in a new transaction (see {@link
 * AuditService}), so a signal for a failed/rolled-back operation is still persisted, and an audit
 * write failure never breaks the caller.
 */
@Component
class AuditSignalListener {

  private final AuditRecorder recorder;

  AuditSignalListener(AuditRecorder recorder) {
    this.recorder = recorder;
  }

  @EventListener
  void on(AuditSignal signal) {
    AuditEvent event =
        AuditEvent.builder()
            .eventType(signal.action())
            .outcome(signal.success() ? AuditOutcome.SUCCESS : AuditOutcome.FAILURE)
            .occurredAt(signal.occurredAt())
            .actor(parseActorType(signal.actorType()), signal.actorId())
            .target(signal.targetType(), signal.targetId())
            .correlationId(CorrelationId.current())
            .detail(signal.attributes().isEmpty() ? null : signal.attributes().toString())
            .build();
    recorder.record(event);
  }

  private static ActorType parseActorType(String value) {
    try {
      return ActorType.valueOf(value);
    } catch (IllegalArgumentException e) {
      return ActorType.SYSTEM;
    }
  }
}
