package uk.gov.hmcts.ccd.domain.model.definition;

import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
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
        objectMapper = JsonMapper.builderWithJackson2Defaults()
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build();
    }

    @Test
    public void ftMasterCaseTypeEventCreateCaseCompareActualAndClone() throws JacksonException {
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
    public void ftComplexCollectionComplexEventcreateSchoolCompareActualAndClone() throws JacksonException {
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
    public void ftConditionalsEventcreateSchoolCompareActualAndClone() throws JacksonException {
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
