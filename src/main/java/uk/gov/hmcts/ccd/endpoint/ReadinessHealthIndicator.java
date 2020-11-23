package uk.gov.hmcts.ccd.endpoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Readiness endpoint that always returns the same as the Liveness.
 * Note this is a *temporary* measure for RDM-10197 and should be deleted
 * when the proper Readiness check is added in RDM-9052.
 */
@Component
public class ReadinessHealthIndicator extends AbstractHealthIndicator {

    @Autowired
    @Qualifier("liveness")
    HealthIndicator livenessIndicator;

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        builder.status(livenessIndicator.health().getStatus());
    }
}
