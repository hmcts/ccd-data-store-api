package uk.gov.hmcts.ccd.domain.service.common;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

class CaseTypeDefinitionServiceTest {

    private static final String CASE_TYPE_ID = "caseTypeId";

    private CaseTypeService subject;

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        subject = new CaseTypeService(null, caseDefinitionRepository);
    }

    @Nested
    @DisplayName("State")
    class State {

        @Test
        @DisplayName("happy path finding a state")
        void happyPathFindState() {
            final CaseTypeDefinition c = new CaseTypeDefinition();
            final CaseStateDefinition caseStateDefinition = buildCaseState("ngitb");
            c.setStates(Arrays.asList(buildCaseState("hemanth"), caseStateDefinition));
            final CaseStateDefinition found = subject.findState(c, "ngitb");
            assertThat(found, is(caseStateDefinition));
        }

        @Test
        @DisplayName("cannot find state by id")
        void cannotFindState() {
            final CaseTypeDefinition c = new CaseTypeDefinition();
            c.setId("nOonEhaStimEtodomYcodEreview");
            c.setStates(Arrays.asList(buildCaseState("hemanth"), buildCaseState("ngitw")));
            final ResourceNotFoundException
                exception = assertThrows(ResourceNotFoundException.class, () -> subject.findState(c, "ngitb"));
            assertThat(exception.getMessage(),
                       is("No state found with id 'ngitb' for case type 'nOonEhaStimEtodomYcodEreview'"));
        }

        private CaseStateDefinition buildCaseState(final String name) {
            final CaseStateDefinition s = new CaseStateDefinition();
            s.setId(name);
            return s;
        }
    }

    @Nested
    @DisplayName("Get case type")
    class GetCaseTypeDefinition {

        @Test
        @DisplayName("should return case type when case type is found for id")
        void shouldReturnCaseType() {
            CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
            when(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).thenReturn(caseTypeDefinition);

            CaseTypeDefinition result = subject.getCaseType(CASE_TYPE_ID);

            assertThat(result, is(caseTypeDefinition));
            verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        }

        @Test
        @DisplayName("should throw exception when case type is not found")
        void shouldThrowException() {
            when(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).thenReturn(null);

            assertThrows(ResourceNotFoundException.class, () -> subject.getCaseType(CASE_TYPE_ID));
        }
    }
}

