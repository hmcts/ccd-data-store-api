package uk.gov.hmcts.ccd.domain.model.definition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;

class WizardPageFieldTest {

    private WizardPageField wizardPageField;

    @BeforeEach
    public void setup() {
        wizardPageField = new WizardPageField();
    }

    @Test
    void shouldFindComplexFieldOverrideByPath() {
        WizardPageComplexFieldOverride override1 = override("LABEL1", "ComplexField.NestedField1");
        WizardPageComplexFieldOverride override2 = override("LABEL2", "ComplexField.NestedField2.SubNestedField");
        wizardPageField.setComplexFieldOverrides(Arrays.asList(override1, override2));

        Optional<WizardPageComplexFieldOverride> result = wizardPageField.getComplexFieldOverride("ComplexField.NestedField2.SubNestedField");

        assertAll(
            () -> assertThat(result.isPresent(), is(true)),
            () -> assertThat(result.get(), is(override2))
        );
    }

    @Test
    void shouldNotFindOverrideForInvalidPath() {
        WizardPageComplexFieldOverride override1 = override("LABEL1", "ComplexField.NestedField1");
        WizardPageComplexFieldOverride override2 = override("LABEL2", "ComplexField.NestedField2.SubNestedField");
        wizardPageField.setComplexFieldOverrides(Arrays.asList(override1, override2));

        Optional<WizardPageComplexFieldOverride> result = wizardPageField.getComplexFieldOverride("InvalidFieldPath");

        assertAll(
            () -> assertThat(result.isPresent(), is(false))
        );
    }

    @Test
    void shouldNotFindOverrideForEmptyList() {
        wizardPageField.setComplexFieldOverrides(Collections.emptyList());

        Optional<WizardPageComplexFieldOverride> result = wizardPageField.getComplexFieldOverride("FieldPath");

        assertAll(
            () -> assertThat(result.isPresent(), is(false))
        );
    }

    private WizardPageComplexFieldOverride override(String label, String fieldPath) {
        WizardPageComplexFieldOverride override = new WizardPageComplexFieldOverride();
        override.setLabel(label);
        override.setComplexFieldElementId(fieldPath);
        return override;
    }
}
