package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
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
        objectMapper = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
            .registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    }

    @Test
    public void ftMasterCaseTypeHashStringComparison() throws JsonProcessingException {
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
    public void beftaCaseType31HashStringComparison() throws JsonProcessingException {
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
    public void ftComplexCrudHashStringComparison() throws JsonProcessingException {
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
