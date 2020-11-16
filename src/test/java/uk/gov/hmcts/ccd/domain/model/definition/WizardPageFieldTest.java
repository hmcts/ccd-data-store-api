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

    private uk.gov.hmcts.ccd.domain.model.definition.WizardPageField wizardPageField;

    @BeforeEach
    public void setup() {
        wizardPageField = new uk.gov.hmcts.ccd.domain.model.definition.WizardPageField();
    }

    @Test
    void shouldFindComplexFieldOverrideByPath() {
        uk.gov.hmcts.ccd.domain.model.definition.WizardPageComplexFieldOverride override1 =
                override("LABEL1", "ComplexField.NestedField1");
        uk.gov.hmcts.ccd.domain.model.definition.WizardPageComplexFieldOverride override2 =
                override("LABEL2", "ComplexField.NestedField2.SubNestedField");
        wizardPageField.setComplexFieldOverrides(Arrays.asList(override1, override2));

        Optional<uk.gov.hmcts.ccd.domain.model.definition.WizardPageComplexFieldOverride> result =
                wizardPageField.getComplexFieldOverride("ComplexField.NestedField2.SubNestedField");

        assertAll(
            () -> assertThat(result.isPresent(), is(true)),
            () -> assertThat(result.get(), is(override2))
        );
    }

    @Test
    void shouldNotFindOverrideForInvalidPath() {
        uk.gov.hmcts.ccd.domain.model.definition.WizardPageComplexFieldOverride override1 =
                override("LABEL1", "ComplexField.NestedField1");
        uk.gov.hmcts.ccd.domain.model.definition.WizardPageComplexFieldOverride override2 =
                override("LABEL2", "ComplexField.NestedField2.SubNestedField");
        wizardPageField.setComplexFieldOverrides(Arrays.asList(override1, override2));

        Optional<uk.gov.hmcts.ccd.domain.model.definition.WizardPageComplexFieldOverride> result =
                wizardPageField.getComplexFieldOverride("InvalidFieldPath");

        assertAll(
            () -> assertThat(result.isPresent(), is(false))
        );
    }

    @Test
    void shouldNotFindOverrideForEmptyList() {
        wizardPageField.setComplexFieldOverrides(Collections.emptyList());

        Optional<uk.gov.hmcts.ccd.domain.model.definition.WizardPageComplexFieldOverride> result =
                wizardPageField.getComplexFieldOverride("FieldPath");

        assertAll(
            () -> assertThat(result.isPresent(), is(false))
        );
    }

    private uk.gov.hmcts.ccd.domain.model.definition.WizardPageComplexFieldOverride override(String label,
                                                                                             String fieldPath) {
        uk.gov.hmcts.ccd.domain.model.definition.WizardPageComplexFieldOverride override =
                new uk.gov.hmcts.ccd.domain.model.definition.WizardPageComplexFieldOverride();
        override.setLabel(label);
        override.setComplexFieldElementId(fieldPath);
        return override;
    }
}
