package uk.gov.hmcts.ccd.infrastructure;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * A request-scoped bean responsible for holding a unique idempotency key for the duration of a single HTTP request.
 * It is used to pass a unique identifier from an orchestrating service (e.g., one handling an event submission) to
 * a cross-cutting component like a Feign {@code RequestInterceptor} without polluting method signatures.
 */
@Component
@RequestScope
@Data
public class IdempotencyKeyHolder {

    private UUID key;

    public void computeAndSetKeyToRequestContext(final String digest) {
        this.key = UUID.nameUUIDFromBytes(digest.getBytes(StandardCharsets.UTF_8));
    }
}
