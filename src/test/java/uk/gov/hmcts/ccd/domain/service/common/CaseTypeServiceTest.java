package uk.gov.hmcts.ccd.domain.service.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.definition.CaseState;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


class CaseTypeServiceTest {

    CaseTypeService subject;

    @BeforeEach
    void setUp() {
        subject = new CaseTypeService(null, null);
    }

    @Nested
    @DisplayName("State")
    class state {

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
    }

    private CaseState buildCaseState(final String name) {
        final CaseState s = new CaseState();
        s.setId(name);
        return s;
    }
}

