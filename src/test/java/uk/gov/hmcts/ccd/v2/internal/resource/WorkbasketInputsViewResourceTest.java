package uk.gov.hmcts.ccd.v2.internal.resource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.WorkbasketInputBuilder.aWorkbasketInput;

import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;

import java.util.List;
import java.util.Optional;
import org.springframework.hateoas.Link;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WorkbasketInputsViewResourceTest {
    private static final String CASE_TYPE_ID = "caseTypeId";
    private static final String LINK_SELF = String.format("/internal/case-types/%s/work-basket-inputs", CASE_TYPE_ID);
    private WorkbasketInput workbasketInput1 = aWorkbasketInput().withFieldId("field1").build();
    private WorkbasketInput workbasketInput2 = aWorkbasketInput().withFieldId("field2").build();
    private WorkbasketInput[] workbasketInputs = new WorkbasketInput[]{workbasketInput1, workbasketInput2};

    @Test
    @DisplayName("should copy workbasket inputs")
    void shouldCopyWorkbasketInputs() {
        final WorkbasketInputsViewResource resource = new WorkbasketInputsViewResource(workbasketInputs, CASE_TYPE_ID);

        List<WorkbasketInputsViewResource.WorkbasketInputView> workbasketInputs =
                Lists.newArrayList(resource.getWorkbasketInputs());
        assertAll(
            () -> assertThat(resource.getWorkbasketInputs(), not(sameInstance(this.workbasketInputs))),
            () -> assertThat(workbasketInputs, hasItems(hasProperty("field", hasProperty("id", is("field1"))),
                                                        hasProperty("field", hasProperty("id", is("field2")))))
        );
    }

    @Test
    @DisplayName("should link to itself")
    void shouldLinkToSelf() {
        final WorkbasketInputsViewResource resource = new WorkbasketInputsViewResource(workbasketInputs, CASE_TYPE_ID);

        Optional<Link> self = resource.getLink("self");

        assertThat(self.get().getHref(), equalTo(LINK_SELF));
    }

}
