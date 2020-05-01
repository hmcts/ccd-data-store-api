package uk.gov.hmcts.ccd.v2.internal.resource;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.hateoas.Link;
import uk.gov.hmcts.ccd.domain.model.aggregated.*;

class CaseHistoryViewResourceTest {

    private static final String REFERENCE = "1234123412341238";
    private static final Long EVENT_ID = 100L;
    private static final String LINK_SELF = String.format("/internal/cases/%s/events/%s", REFERENCE, EVENT_ID);

    @Mock
    private CaseViewType caseType;

    @Mock
    private List<CaseViewField> metadataFields;

    private CaseViewTab[] tabs;
    private CaseViewEvent event;

    private CaseHistoryView caseHistoryView;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        tabs = new CaseViewTab[]{};
        event = newCaseViewEvent();
        caseHistoryView = newCaseHistoryView();
    }

    @Test
    @DisplayName("should copy case view")
    void shouldCopyCaseView() {
        final CaseHistoryViewResource resource = new CaseHistoryViewResource(caseHistoryView, REFERENCE);

        assertAll(
            () -> assertThat(resource.getCaseId(), equalTo(REFERENCE)),
            () -> assertThat(resource.getCaseType(), sameInstance(caseType)),
            () -> assertThat(resource.getTabs(), sameInstance(tabs)),
            () -> assertThat(resource.getMetadataFields(), sameInstance(metadataFields)),
            () -> assertThat(resource.getEvent(), sameInstance(event))
        );
    }

    @Test
    @DisplayName("should link to itself")
    void shouldLinkToSelf() {
        final CaseHistoryViewResource resource = new CaseHistoryViewResource(caseHistoryView, REFERENCE);

        Optional<Link> self = resource.getLink("self");
        assertThat(self.get().getHref(), equalTo(LINK_SELF));
    }

    private CaseViewEvent newCaseViewEvent() {
        final CaseViewEvent caseViewEvent = new CaseViewEvent();
        caseViewEvent.setId(EVENT_ID);
        return caseViewEvent;
    }

    private CaseHistoryView newCaseHistoryView() {
        final CaseHistoryView caseHistoryView = new CaseHistoryView();

        caseHistoryView.setCaseId(REFERENCE);
        caseHistoryView.setCaseType(caseType);
        caseHistoryView.setTabs(tabs);
        caseHistoryView.setMetadataFields(metadataFields);
        caseHistoryView.setEvent(event);

        return caseHistoryView;
    }
}
