package uk.gov.hmcts.ccd.v2.external.controller;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.infrastructure.IdempotencyKeyHolder;

@Component
@Profile("SECURITY_MOCK")
public class ContractTestIdempotencyKeyHolder extends IdempotencyKeyHolder {

    public void resetKey() {
        key = null;
    }

    @Override
    public void computeAndSetKeyToRequestContext(final String digest) {
        key = UUID.nameUUIDFromBytes(digest.getBytes(StandardCharsets.UTF_8));
    }
}
