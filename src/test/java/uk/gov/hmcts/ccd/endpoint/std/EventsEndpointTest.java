package uk.gov.hmcts.ccd.endpoint.std;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.listevents.ListEventsOperation;

class EventsEndpointTest {

    private static final Integer UID = 1231;
    private static final String JURISDICTION_ID = "Probate";
    private static final String CASE_TYPE_ID = "GrantOnly";
    private static final String CASE_ID = "1234qwer5678tyui";

    @Mock
    private ListEventsOperation listEventsOperation;

    private EventsEndpoint endpoint;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        endpoint = new EventsEndpoint(listEventsOperation);
    }

    @Test
    void shouldReturnEventsForCase() {
        final List<AuditEvent> events = new ArrayList<>();
        doReturn(events).when(listEventsOperation).execute(JURISDICTION_ID, CASE_TYPE_ID, CASE_ID);

        final List<AuditEvent> output = endpoint.findEventDetailsForCase(UID,
            JURISDICTION_ID,
            CASE_TYPE_ID,
            CASE_ID);

        assertThat(output, sameInstance(events));
        verify(listEventsOperation).execute(JURISDICTION_ID, CASE_TYPE_ID, CASE_ID);
    }

}
