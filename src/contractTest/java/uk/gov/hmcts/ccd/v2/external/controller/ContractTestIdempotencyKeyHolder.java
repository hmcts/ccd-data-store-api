package uk.gov.hmcts.ccd.v2.external.controller;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.infrastructure.IdempotencyKeyHolder;

@Component
@Profile("SECURITY_MOCK")
@Primary
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class ContractTestIdempotencyKeyHolder extends IdempotencyKeyHolder {

    private UUID contractTestKey;

    public void resetKey() {
        contractTestKey = null;
    }

    @Override
    public void computeAndSetKeyToRequestContext(final String digest) {
        contractTestKey = UUID.nameUUIDFromBytes(digest.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public UUID getKey() {
        return contractTestKey;
    }
}
