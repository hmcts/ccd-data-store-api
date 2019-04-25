package uk.gov.hmcts.ccd.domain.model.definition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COMPLEX;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

class CaseTypeTest {

    private static final String TEXT_TYPE = "Text";
    private static final String PERSON = "Person";
    private static final String DEBTOR_DETAILS = "Debtor details";
    private static final String NAME = "Name";
    private static final String SURNAME = "Surname";

    private CaseField name = newCaseField().withId(NAME).withFieldType(aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build()).build();
    private CaseField surname = newCaseField().withId(SURNAME).withFieldType(aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build()).build();
    private FieldType personFieldType = aFieldType().withId(PERSON).withType(COMPLEX).withComplexField(name).withComplexField(surname).build();
    private CaseField person = newCaseField().withId(PERSON).withFieldType(personFieldType).build();

    private FieldType debtorFieldType = aFieldType().withId(DEBTOR_DETAILS).withType(COMPLEX).withComplexField(person).build();
    private CaseField debtorDetails = newCaseField().withId(DEBTOR_DETAILS).withFieldType(debtorFieldType).build();

    private CaseType caseType;

    @Nested
    @DisplayName("CaseField tests")
    class FindNestedElementsTest {

        @BeforeEach
        void setUp() {
            caseType = new CaseType();
            caseType.setCaseFields(Collections.singletonList(debtorDetails));
        }

        @Test
        @DisplayName("returns empty optional when caseFieldId is invalid")
        void getFieldTypeByPathReturnsEmptyOptionalWhenCaseFieldIdIsInvalid() {
            Optional<CaseField> caseFieldOptional = caseType.getCaseFieldByPath("invalidId", null);

            assertThat(Optional.empty(), is(caseFieldOptional));
        }

        @Test
        @DisplayName("returns current caseField if path is null")
        void getFieldTypeByPathReturnsSameWhenPathIsNull() {
            Optional<CaseField> caseFieldOptional = caseType.getCaseFieldByPath(debtorDetails.getId(), null);

            assertTrue(caseFieldOptional.isPresent());
            assertThat(debtorDetails, is(caseFieldOptional.get()));
        }

        @Test
        @DisplayName("returns nested caseField if valid path is provided")
        void getFieldTypeByPathReturnsNestedCaseFieldForValidPath() {
            String path = PERSON + "." + NAME;
            Optional<CaseField> caseFieldOptional = caseType.getCaseFieldByPath(debtorDetails.getId(), path);

            assertTrue(caseFieldOptional.isPresent());
            assertThat(name, is(caseFieldOptional.get()));
        }
    }
}
