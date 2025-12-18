package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static uk.gov.hmcts.ccd.TestFixtures.fromFileAsString;

class WizardPageTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
            .registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    }

    @Test
    public void ftMasterCaseTypeEventCreateCaseCompareActualAndClone() throws JsonProcessingException {
        String fileContent = fromFileAsString("tests/FT-MasterCaseType-event-createCase-payload.json");

        WizardPageCollection wizardPageCollection = objectMapper.readValue(fileContent, WizardPageCollection.class);
        List<WizardPage> wizardPages = wizardPageCollection.getWizardPages();
        WizardPage wizardPage = wizardPages.get(0);
        WizardPage copiedWizardPage = wizardPage.createCopy();

        assertNotEquals(wizardPage.hashCode(), copiedWizardPage.hashCode());

        String originalJson = objectMapper.writeValueAsString(wizardPage);
        String copiedJson = objectMapper.writeValueAsString(copiedWizardPage);

        String originalJsonHash256 = DigestUtils.sha256Hex(originalJson);
        String copiedJsonHash256 = DigestUtils.sha256Hex(copiedJson);

        assertEquals(originalJsonHash256, copiedJsonHash256);
    }

    @Test
    public void ftComplexCollectionComplexEventcreateSchoolCompareActualAndClone() throws JsonProcessingException {
        String fileContent = fromFileAsString("tests/FT-ComplexCollectionComplex-event-createSchool-payload.json");

        WizardPageCollection wizardPageCollection = objectMapper.readValue(fileContent, WizardPageCollection.class);
        List<WizardPage> wizardPages = wizardPageCollection.getWizardPages();
        WizardPage wizardPage = wizardPages.get(0);
        WizardPage copiedWizardPage = wizardPage.createCopy();

        assertNotEquals(wizardPage.hashCode(), copiedWizardPage.hashCode());

        String originalJson = objectMapper.writeValueAsString(wizardPage);
        String copiedJson = objectMapper.writeValueAsString(copiedWizardPage);

        String originalJsonHash256 = DigestUtils.sha256Hex(originalJson);
        String copiedJsonHash256 = DigestUtils.sha256Hex(copiedJson);

        assertEquals(originalJsonHash256, copiedJsonHash256);
    }

    @Test
    public void ftConditionalsEventcreateSchoolCompareActualAndClone() throws JsonProcessingException {
        String fileContent = fromFileAsString("tests/FT-Conditionals-event-createCase-payload.json");

        WizardPageCollection wizardPageCollection = objectMapper.readValue(fileContent, WizardPageCollection.class);
        List<WizardPage> wizardPages = wizardPageCollection.getWizardPages();
        WizardPage wizardPage = wizardPages.get(0);
        WizardPage copiedWizardPage = wizardPage.createCopy();

        assertNotEquals(wizardPage.hashCode(), copiedWizardPage.hashCode());

        String originalJson = objectMapper.writeValueAsString(wizardPage);
        String copiedJson = objectMapper.writeValueAsString(copiedWizardPage);

        String originalJsonHash256 = DigestUtils.sha256Hex(originalJson);
        String copiedJsonHash256 = DigestUtils.sha256Hex(copiedJson);

        assertEquals(originalJsonHash256, copiedJsonHash256);
    }
}
