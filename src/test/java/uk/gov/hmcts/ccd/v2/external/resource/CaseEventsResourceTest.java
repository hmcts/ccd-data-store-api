package uk.gov.hmcts.ccd.v2.external.resource;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.hateoas.Link;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;

class CaseEventsResourceTest {
    private static final String REFERENCE = "1234123412341238";
    private static final String LINK_SELF = String.format("/cases/%s/events", REFERENCE);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

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

        Optional<Link> self = resource.getLink("self");
        assertThat(self.get().getHref(), equalTo(LINK_SELF));
    }
}
