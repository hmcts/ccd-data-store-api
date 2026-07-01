package uk.gov.hmcts.ccd.infrastructure;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdempotencyKeyHolderTest {

    @Test
    void computeAndSetKeyToRequestContextInitialisesKeyOnce() {
        IdempotencyKeyHolder holder = new IdempotencyKeyHolder();

        holder.computeAndSetKeyToRequestContext("digest");

        UUID expected = UUID.nameUUIDFromBytes("digest".getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, holder.getKey());
    }

    @Test
    void computeAndSetKeyToRequestContextThrowsWhenKeyAlreadyInitialised() {
        IdempotencyKeyHolder holder = new IdempotencyKeyHolder();
        holder.computeAndSetKeyToRequestContext("digest");

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> holder.computeAndSetKeyToRequestContext("another-digest")
        );

        assertEquals("Idempotency key has already been initialised for this request", exception.getMessage());
        UUID expected = UUID.nameUUIDFromBytes("digest".getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, holder.getKey());
    }
}
