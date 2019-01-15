package uk.gov.hmcts.ccd.v2.internal.resource;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.WorkbasketInputBuilder.aWorkbasketInput;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;

class UIWorkbasketInputsResourceTest {
    private static final String CASE_TYPE_ID = "caseTypeId";
    private static final String LINK_SELF = String.format("/internal/case-types/%s/work-basket-inputs", CASE_TYPE_ID);
    private WorkbasketInput workbasketInput1 = aWorkbasketInput().withFieldId("field1").build();
    private WorkbasketInput workbasketInput2 = aWorkbasketInput().withFieldId("field2").build();
    private WorkbasketInput[] workbasketInputs = new WorkbasketInput[]{workbasketInput1, workbasketInput2};

    @Test
    @DisplayName("should copy workbasket inputs")
    void shouldCopyUserProfile() {
        final UIWorkbasketInputsResource resource = new UIWorkbasketInputsResource(workbasketInputs, CASE_TYPE_ID);

        List<UIWorkbasketInputsResource.UIWorkbasketInput> workbasketInputs = Lists.newArrayList(resource.getWorkbasketInputs());
        assertAll(
            () -> assertThat(resource.getWorkbasketInputs(), not(sameInstance(this.workbasketInputs))),
            () -> assertThat(workbasketInputs, hasItems(hasProperty("field", hasProperty("id", is("field1"))),
                                                        hasProperty("field", hasProperty("id", is("field2")))))
        );
    }

    @Test
    @DisplayName("should link to itself")
    void shouldLinkToSelf() {
        final UIWorkbasketInputsResource resource = new UIWorkbasketInputsResource(workbasketInputs, CASE_TYPE_ID);

        assertThat(resource.getLink("self").getHref(), equalTo(LINK_SELF));
    }

}
