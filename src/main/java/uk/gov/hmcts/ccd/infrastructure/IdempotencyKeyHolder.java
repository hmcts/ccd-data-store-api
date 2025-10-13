package uk.gov.hmcts.ccd.infrastructure;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * A request-scoped bean responsible for holding a unique idempotency key that identifies a CCD event operation
 * as the hash of the event's start event token.
 * It is used to pass a unique identifier from an orchestrating service (e.g., one handling an event submission) to
 * a cross-cutting component like a Feign {@code RequestInterceptor} without polluting method signatures.
 */
@Component
@RequestScope
@Getter
public class IdempotencyKeyHolder {

    private UUID key;

    public void computeAndSetKeyToRequestContext(final String digest) {
        if (this.key != null) {
            throw new IllegalStateException("Idempotency key has already been initialised for this request");
        }
        this.key = UUID.nameUUIDFromBytes(digest.getBytes(StandardCharsets.UTF_8));
    }
}
