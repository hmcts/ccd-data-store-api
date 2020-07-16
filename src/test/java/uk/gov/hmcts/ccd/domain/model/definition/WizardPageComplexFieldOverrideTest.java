package uk.gov.hmcts.ccd.domain.model.definition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;

class WizardPageComplexFieldOverrideTest {

    private WizardPageComplexFieldOverride override;

    @BeforeEach
    public void setup() {
        override = new WizardPageComplexFieldOverride();
    }

    @Test
    void shouldReturnCorrectDisplayContextEnumForReadOnlyContext() {
        override.setDisplayContext("READONLY");

        DisplayContext result = override.displayContextType();

        assertThat(result, is(DisplayContext.READONLY));
    }

    @Test
    void shouldReturnCorrectDisplayContextEnumForMandatoryContext() {
        override.setDisplayContext("MANDATORY");

        DisplayContext result = override.displayContextType();

        assertThat(result, is(DisplayContext.MANDATORY));
    }

    @Test
    void shouldReturnCorrectDisplayContextEnumForOptionalContext() {
        override.setDisplayContext("OPTIONAL");

        DisplayContext result = override.displayContextType();

        assertThat(result, is(DisplayContext.OPTIONAL));
    }

    @Test
    void shouldReturnNullForHiddenContext() {
        override.setDisplayContext("HIDDEN");

        DisplayContext result = override.displayContextType();

        assertNull(result);
    }
}
