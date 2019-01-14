package uk.gov.hmcts.ccd.v2.internal.controller;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.WorkbasketInputBuilder.aWorkbasketInput;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;
import uk.gov.hmcts.ccd.domain.service.aggregated.FindWorkbasketInputOperation;
import uk.gov.hmcts.ccd.v2.internal.resource.UIWorkbasketInputsResource;

class UIDefinitionControllerTest {
    private static final String CASE_TYPE_ID = "caseTypeId";

    private WorkbasketInput workbasketInput1 = aWorkbasketInput().withFieldId("field1").build();
    private WorkbasketInput workbasketInput2 = aWorkbasketInput().withFieldId("field2").build();

    private final List<WorkbasketInput> workbasketInputs = Lists.newArrayList(workbasketInput1, workbasketInput2);

    @Mock
    private FindWorkbasketInputOperation findWorkbasketInputOperation;
    @InjectMocks
    private UIDefinitionController uiDefinitionController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(findWorkbasketInputOperation.execute(CASE_TYPE_ID, CAN_READ)).thenReturn(workbasketInputs);
    }


    @Nested
    @DisplayName("GET /internal/case-types/{caseTypeId}/work-basket-inputs")
    class GetWorkbasketInputsDetails {
        @Test
        @DisplayName("should return 200 when case found")
        void caseFound() {
            final ResponseEntity<UIWorkbasketInputsResource> response = uiDefinitionController.getWorkbasketInputsDetails(CASE_TYPE_ID);

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
                () -> {
                    UIWorkbasketInputsResource.UIWorkbasketInput[] workbasketInputs = response.getBody().getWorkbasketInputs();
                    assertThat(Lists.newArrayList(workbasketInputs), hasItems(hasProperty("field", hasProperty("id", is("field1"))),
                                                                              hasProperty("field", hasProperty("id", is("field2")))));
                }
            );
        }

        @Test
        @DisplayName("should propagate exception")
        void shouldPropagateExceptionWhenThrown() {
            when(findWorkbasketInputOperation.execute(CASE_TYPE_ID, CAN_READ)).thenThrow(Exception.class);

            assertThrows(Exception.class,
                         () -> uiDefinitionController.getWorkbasketInputsDetails(CASE_TYPE_ID));
        }
    }
}
