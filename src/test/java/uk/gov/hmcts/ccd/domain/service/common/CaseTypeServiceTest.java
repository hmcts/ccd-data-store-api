package uk.gov.hmcts.ccd.domain.service.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseState;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CaseTypeServiceTest {

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
            final CaseType c = new CaseType();
            final CaseState caseState = buildCaseState("ngitb");
            c.setStates(Arrays.asList(buildCaseState("hemanth"), caseState));
            final CaseState found = subject.findState(c, "ngitb");
            assertThat(found, is(caseState));
        }

        @Test
        @DisplayName("cannot find state by id")
        void cannotFindState() {
            final CaseType c = new CaseType();
            c.setId("nOonEhaStimEtodomYcodEreview");
            c.setStates(Arrays.asList(buildCaseState("hemanth"), buildCaseState("ngitw")));
            final ResourceNotFoundException
                exception = assertThrows(ResourceNotFoundException.class, () -> subject.findState(c, "ngitb"));
            assertThat(exception.getMessage(),
                       is("No state found with id 'ngitb' for case type 'nOonEhaStimEtodomYcodEreview'"));
        }

        private CaseState buildCaseState(final String name) {
            final CaseState s = new CaseState();
            s.setId(name);
            return s;
        }
    }

    @Nested
    @DisplayName("Get case type")
    class GetCaseType {

        @Test
        @DisplayName("should return case type when case type is found for id")
        void shouldReturnCaseType() {
            CaseType caseType = new CaseType();
            when(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).thenReturn(caseType);

            CaseType result = subject.getCaseType(CASE_TYPE_ID);

            assertThat(result, is(caseType));
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

