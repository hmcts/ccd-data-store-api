package uk.gov.hmcts.ccd.v2.internal.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.hateoas.Link;
import uk.gov.hmcts.ccd.domain.model.aggregated.*;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("UICaseViewResource")
class UICaseViewResourceTest {
    private static final String REFERENCE = "1234123412341238";
    private static final String LINK_SELF = String.format("/internal/cases/%s", REFERENCE);

    @Mock
    private CaseViewType caseType;

    @Mock
    private List<CaseViewField> metadataFields;

    @Mock
    private ProfileCaseState state;

    private CaseViewTab[] tabs;
    private CaseViewTrigger[] triggers;
    private CaseViewEvent[] events;

    private CaseView caseView;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        mockArrays();

        caseView = newCaseView();
    }

    @Test
    @DisplayName("should copy case view")
    void shouldCopyCaseView() {
        final UICaseViewResource resource = new UICaseViewResource(caseView);

        assertAll(
            () -> assertThat(resource.getReference(), equalTo(REFERENCE)),
            () -> assertThat(resource.getCaseType(), sameInstance(caseType)),
            () -> assertThat(resource.getTabs(), sameInstance(tabs)),
            () -> assertThat(resource.getMetadataFields(), sameInstance(metadataFields)),
            () -> assertThat(resource.getState(), sameInstance(state)),
            () -> assertThat(resource.getTriggers(), sameInstance(triggers)),
            () -> assertThat(resource.getEvents(), sameInstance(events))
        );
    }

    @Test
    @DisplayName("should link to itself")
    void shouldLinkToSelf() {
        final UICaseViewResource resource = new UICaseViewResource(caseView);

        Optional<Link> self = resource.getLink("self");
        assertThat(self.get().getHref(), equalTo(LINK_SELF));
    }

    private void mockArrays() {
        tabs = new CaseViewTab[]{};
        triggers = new CaseViewTrigger[]{};
        events = new CaseViewEvent[]{};
    }

    private CaseView newCaseView() {
        final CaseView caseView = new CaseView();

        caseView.setCaseId(REFERENCE);
        caseView.setCaseType(caseType);
        caseView.setTabs(tabs);
        caseView.setMetadataFields(metadataFields);
        caseView.setState(state);
        caseView.setTriggers(triggers);
        caseView.setEvents(events);

        return caseView;
    }

}
