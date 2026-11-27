package uk.gov.hmcts.ccd.datastore.befta;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class DateCaseClosedCleanupContext {

    private static final ThreadLocal<ScenarioCleanup> CURRENT = new ThreadLocal<>();

    private DateCaseClosedCleanupContext() {
    }

    static void beginScenario() {
        CURRENT.set(new ScenarioCleanup());
    }

    static void registerCaseReference(Object reference) {
        ScenarioCleanup cleanup = CURRENT.get();
        if (cleanup == null) {
            throw new IllegalStateException("No closed-case cleanup context is active");
        }
        if (reference == null) {
            throw new IllegalArgumentException("Case reference for closed-case cleanup cannot be null");
        }
        cleanup.caseReferences.add(Long.valueOf(reference.toString()));
    }

    static void setAccessToken(String accessToken) {
        ScenarioCleanup cleanup = CURRENT.get();
        if (cleanup != null && accessToken != null) {
            cleanup.accessToken = accessToken;
        }
    }

    static ScenarioCleanup currentScenario() {
        return CURRENT.get();
    }

    static void clear() {
        CURRENT.remove();
    }

    static final class ScenarioCleanup {
        private final Set<Long> caseReferences = new LinkedHashSet<>();
        private String accessToken;

        List<Long> getCaseReferences() {
            return List.copyOf(caseReferences);
        }

        String getAccessToken() {
            return accessToken;
        }
    }
}
