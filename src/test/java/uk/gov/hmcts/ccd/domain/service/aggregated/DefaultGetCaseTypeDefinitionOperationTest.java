package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;

public class DefaultGetCaseTypeDefinitionOperationTest {

    private static final String CASE_TYPE_ID = "caseTypeId";

    @Mock
    private CaseTypeService caseTypeService;

    @InjectMocks
    private DefaultGetCaseTypeOperation operation;

    @Mock
    private CaseTypeDefinition caseTypeDefinitionMock;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldInvokeCaseTypeService() {

        when(caseTypeService.getCaseType(CASE_TYPE_ID)).thenReturn(caseTypeDefinitionMock);

        Optional<CaseTypeDefinition> caseTypeOptional = operation.execute(CASE_TYPE_ID, null);

        assertThat(caseTypeOptional, equalTo(Optional.of(caseTypeDefinitionMock)));
    }

}
