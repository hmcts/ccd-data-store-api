package uk.gov.hmcts.ccd.v2.internal.resource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.WorkbasketInputBuilder.aWorkbasketInput;

class UIWorkbasketInputsResourceTest {
    private static final String CASE_TYPE_ID = "caseTypeId";
    private static final String LINK_SELF = String.format("/internal/case-types/%s/work-basket-inputs", CASE_TYPE_ID);
    private WorkbasketInput workbasketInput1 = aWorkbasketInput().build();
    private WorkbasketInput workbasketInput2 = aWorkbasketInput().build();
    private WorkbasketInput[] workbasketInputs = new WorkbasketInput[] {workbasketInput1, workbasketInput2};

    @Test
    @DisplayName("should copy workbasket inputs")
    void shouldCopyUserProfile() {
        final UIWorkbasketInputsResource resource = new UIWorkbasketInputsResource(workbasketInputs, CASE_TYPE_ID);

        assertAll(
            () -> assertThat(resource.getWorkbasketInputs(), sameInstance(workbasketInputs))
        );
    }

    @Test
    @DisplayName("should link to itself")
    void shouldLinkToSelf() {
        final UIWorkbasketInputsResource resource = new UIWorkbasketInputsResource(workbasketInputs, CASE_TYPE_ID);

        assertThat(resource.getLink("self").getHref(), equalTo(LINK_SELF));
    }

}
