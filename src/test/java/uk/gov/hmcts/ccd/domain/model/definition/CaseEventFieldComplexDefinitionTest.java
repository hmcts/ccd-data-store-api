package uk.gov.hmcts.ccd.domain.model.definition;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class CaseEventFieldComplexDefinitionTest {
    private CaseEventFieldComplexDefinition caseEventFieldComplexDefinition;

    @Test
    void testDataIsSetCorrectly() {
        caseEventFieldComplexDefinition = CaseEventFieldComplexDefinition
            .builder()
            .defaultValue("")
            .displayContextParameter("#DATETIMEDISPLAY(d M yy)")
            .order(1)
            .reference("Reference")
            .retainHiddenValue(true)
            .build();

        assertThat(caseEventFieldComplexDefinition.getRetainHiddenValue(), equalTo(true));
        assertThat(caseEventFieldComplexDefinition.getDefaultValue(), equalTo(""));
        assertThat(caseEventFieldComplexDefinition.getDisplayContextParameter(), equalTo("#DATETIMEDISPLAY(d M yy)"));
        assertThat(caseEventFieldComplexDefinition.getOrder(), equalTo(1));
        assertThat(caseEventFieldComplexDefinition.getReference(), equalTo("Reference"));

    }
}
