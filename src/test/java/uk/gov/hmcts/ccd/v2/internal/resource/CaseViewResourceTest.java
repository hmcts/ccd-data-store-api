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
class CaseViewResourceTest {
    private static final String REFERENCE = "1234123412341238";
    private static final String LINK_SELF = String.format("/internal/cases/%s", REFERENCE);

    @Mock
    private CaseViewType caseType;

    @Mock
    private List<CaseViewField> metadataFields;

    @Mock
    private ProfileCaseState state;

    private CaseViewTab[] tabs;
    private CaseViewActionableEvent[] caseViewActionableEvents;
    private CaseViewEvent[] caseViewEvents;

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
        final CaseViewResource resource = new CaseViewResource(caseView);

        assertAll(
            () -> assertThat(resource.getReference(), equalTo(REFERENCE)),
            () -> assertThat(resource.getCaseType(), sameInstance(caseType)),
            () -> assertThat(resource.getTabs(), sameInstance(tabs)),
            () -> assertThat(resource.getMetadataFields(), sameInstance(metadataFields)),
            () -> assertThat(resource.getState(), sameInstance(state)),
            () -> assertThat(resource.getCaseViewActionableEvents(), sameInstance(caseViewActionableEvents)),
            () -> assertThat(resource.getCaseViewEvents(), sameInstance(caseViewEvents))
        );
    }

    @Test
    @DisplayName("should link to itself")
    void shouldLinkToSelf() {
        final CaseViewResource resource = new CaseViewResource(caseView);

        Optional<Link> self = resource.getLink("self");
        assertThat(self.get().getHref(), equalTo(LINK_SELF));
    }

    private void mockArrays() {
        tabs = new CaseViewTab[]{};
        caseViewActionableEvents = new CaseViewActionableEvent[]{};
        caseViewEvents = new CaseViewEvent[]{};
    }

    private CaseView newCaseView() {
        final CaseView caseView = new CaseView();

        caseView.setCaseId(REFERENCE);
        caseView.setCaseType(caseType);
        caseView.setTabs(tabs);
        caseView.setMetadataFields(metadataFields);
        caseView.setState(state);
        caseView.setActionableEvents(caseViewActionableEvents);
        caseView.setEvents(caseViewEvents);

        return caseView;
    }

}
