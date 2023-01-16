package uk.gov.hmcts.ccd.domain.model.definition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

class CaseTypeDefinitionTest {

    private static final String TEXT_TYPE = "Text";
    private static final String NAME = "Name";
    private static final String SURNAME = "Surname";

    private uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition name =
            newCaseField().withId(NAME).withFieldType(aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build())
                    .build();
    private uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition surname =
            newCaseField().withId(SURNAME).withFieldType(aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build())
                    .build();

    private CaseTypeDefinition caseTypeDefinition;

    @Nested
    @DisplayName("CaseField tests")
    class FindNestedElementsTest {

        @BeforeEach
        void setUp() {
            caseTypeDefinition = CaseTypeDefinition.builder()
                .caseFieldDefinitions(List.of(name, surname))
                .build();
        }

        @Test
        @DisplayName("returns caseField optional for a valid caseFieldId")
        void getCaseFieldReturnsCaseFieldOptionalForValidCaseFieldId() {
            Optional<uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition> caseFieldOptional =
                    caseTypeDefinition.getCaseField(surname.getId());

            assertTrue(caseFieldOptional.isPresent());
            assertThat(surname, is(caseFieldOptional.get()));
        }

        @Test
        @DisplayName("returns empty optional when caseFieldId is invalid")
        void getCaseFieldReturnsEmptyOptionalWhenCaseFieldIdIsInvalid() {
            Optional<uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition> caseFieldOptional =
                    caseTypeDefinition.getCaseField("invalidId");

            assertThat(Optional.empty(), is(caseFieldOptional));
        }
    }
}
