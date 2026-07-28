package uk.gov.hmcts.ccd.domain.model.definition;

import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.ccd.TestFixtures.fromFileAsString;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

class CaseTypeDefinitionTest {

    private static final String TEXT_TYPE = "Text";
    private static final String NAME = "Name";
    private static final String SURNAME = "Surname";

    private final uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition name =
            newCaseField().withId(NAME).withFieldType(aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build())
                    .build();
    private final uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition surname =
            newCaseField().withId(SURNAME).withFieldType(aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build())
                    .build();

    private uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition caseTypeDefinition;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builderWithJackson2Defaults()
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build();
    }

    @Test
    public void ftMasterCaseTypeHashStringComparison() throws JacksonException {
        String fileContent = fromFileAsString("tests/FT-MasterCaseType-payload.json");

        CaseTypeDefinition caseTypeDefinition = objectMapper.readValue(fileContent, CaseTypeDefinition.class);
        CaseTypeDefinition copiedCaseTypeDefinition = caseTypeDefinition.createCopy();

        assertNotEquals(caseTypeDefinition.hashCode(), copiedCaseTypeDefinition.hashCode());

        String originalJson = objectMapper.writeValueAsString(caseTypeDefinition);
        String copiedJson = objectMapper.writeValueAsString(copiedCaseTypeDefinition);

        String originalJsonHash256 = DigestUtils.sha256Hex(originalJson);
        String copiedJsonHash256 = DigestUtils.sha256Hex(copiedJson);

        assertEquals(originalJsonHash256, copiedJsonHash256);
    }

    @Test
    public void beftaCaseType31HashStringComparison() throws JacksonException {
        String fileContent = fromFileAsString("tests/BEFTA-CASETYPE-3-1-payload.json");

        CaseTypeDefinition caseTypeDefinition = objectMapper.readValue(fileContent, CaseTypeDefinition.class);
        CaseTypeDefinition copiedCaseTypeDefinition = caseTypeDefinition.createCopy();

        assertNotEquals(caseTypeDefinition.hashCode(), copiedCaseTypeDefinition.hashCode());

        String originalJson = objectMapper.writeValueAsString(caseTypeDefinition);
        String copiedJson = objectMapper.writeValueAsString(copiedCaseTypeDefinition);

        String originalJsonHash256 = DigestUtils.sha256Hex(originalJson);
        String copiedJsonHash256 = DigestUtils.sha256Hex(copiedJson);

        assertEquals(originalJsonHash256, copiedJsonHash256);
    }

    @Test
    public void ftComplexCrudHashStringComparison() throws JacksonException {
        String fileContent = fromFileAsString("tests/FT-ComplexCRUD-payload.json");

        CaseTypeDefinition caseTypeDefinition = objectMapper.readValue(fileContent, CaseTypeDefinition.class);
        CaseTypeDefinition copiedCaseTypeDefinition = caseTypeDefinition.createCopy();

        assertNotEquals(caseTypeDefinition.hashCode(), copiedCaseTypeDefinition.hashCode());

        String originalJson = objectMapper.writeValueAsString(caseTypeDefinition);
        String copiedJson = objectMapper.writeValueAsString(copiedCaseTypeDefinition);

        String originalJsonHash256 = DigestUtils.sha256Hex(originalJson);
        String copiedJsonHash256 = DigestUtils.sha256Hex(copiedJson);

        assertEquals(originalJsonHash256, copiedJsonHash256);
    }

    @Nested
    @DisplayName("CaseField tests")
    class FindNestedElementsTest {

        @BeforeEach
        void setUp() {
            caseTypeDefinition = new uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition();
            caseTypeDefinition.setCaseFieldDefinitions(Arrays.asList(name, surname));
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
