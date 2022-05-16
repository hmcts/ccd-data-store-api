package uk.gov.hmcts.ccd.v2.external.resource;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.Link;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;

class CaseEventDefinitionsResourceTest {
    private static final String REFERENCE = "1234123412341238";
    private static final String LINK_SELF = String.format("/cases/%s/events", REFERENCE);

    @Test
    @DisplayName("should copy empty audit events")
    void shouldCopyEmptyAuditEvents() {
        List<AuditEvent> auditEvents = Lists.newArrayList();
        final CaseEventsResource resource = new CaseEventsResource(REFERENCE, auditEvents);

        assertAll(
            () -> assertThat(resource.getAuditEvents().size(), is(0))
        );
    }

    @Test
    @DisplayName("should copy audit events")
    void shouldCopyAuditEvents() {
        List<AuditEvent> auditEvents = Lists.newArrayList(new AuditEvent(), new AuditEvent());
        final CaseEventsResource resource = new CaseEventsResource(REFERENCE, auditEvents);

        assertAll(
            () -> assertThat(resource.getAuditEvents().size(), is(2))
        );
    }

    @Test
    @DisplayName("should link to itself")
    void shouldLinkToSelf() {
        List<AuditEvent> auditEvents = Lists.newArrayList();
        final CaseEventsResource resource = new CaseEventsResource(REFERENCE, auditEvents);

        final Optional<Link> self = resource.getLink("self");
        self.ifPresent(link -> assertThat(link.getHref(), endsWith(LINK_SELF)));
    }
}
