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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static uk.gov.hmcts.ccd.TestFixtures.fromFileAsString;

class JurisdictionDefinitionTest {
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
    public void autoTest1JurisdictionCompareActualAndClone() throws JacksonException {
        String fileContent = fromFileAsString("tests/AUTOTEST1-jurisdiction-payload.json");

        JurisdictionDefinition jurisdictionDefinition = objectMapper
            .readValue(fileContent, JurisdictionDefinition.class);
        JurisdictionDefinition copiedJurisdictionDefinition  = jurisdictionDefinition.createCopy();

        assertNotEquals(jurisdictionDefinition.hashCode(), copiedJurisdictionDefinition.hashCode());

        String originalJson = objectMapper.writeValueAsString(jurisdictionDefinition);
        String copiedJson = objectMapper.writeValueAsString(copiedJurisdictionDefinition);

        String originalJsonHash256 = DigestUtils.sha256Hex(originalJson);
        String copiedJsonHash256 = DigestUtils.sha256Hex(copiedJson);

        assertNotEquals(jurisdictionDefinition.hashCode(), copiedJurisdictionDefinition.hashCode());
        assertEquals(originalJsonHash256, copiedJsonHash256);
    }

    @Test
    public void beftaMasterJurisdictionCompareActualAndClone() throws JacksonException {
        String fileContent = fromFileAsString("tests/BEFTA_MASTER-jurisdiction-payload.json");

        JurisdictionDefinition jurisdictionDefinition = objectMapper
            .readValue(fileContent, JurisdictionDefinition.class);
        JurisdictionDefinition copiedJurisdictionDefinition  = jurisdictionDefinition.createCopy();

        assertNotEquals(jurisdictionDefinition.hashCode(), copiedJurisdictionDefinition.hashCode());

        String originalJson = objectMapper.writeValueAsString(jurisdictionDefinition);
        String copiedJson = objectMapper.writeValueAsString(copiedJurisdictionDefinition);

        String originalJsonHash256 = DigestUtils.sha256Hex(originalJson);
        String copiedJsonHash256 = DigestUtils.sha256Hex(copiedJson);

        assertNotEquals(jurisdictionDefinition.hashCode(), copiedJurisdictionDefinition.hashCode());
        assertEquals(originalJsonHash256, copiedJsonHash256);
    }

    @Test
    public void beftaJurisdiction3JurisdictionCompareActualAndClone() throws JacksonException {
        String fileContent = fromFileAsString("tests/BEFTA_JURISDICTION_3-jurisdiction-payload.json");

        JurisdictionDefinition jurisdictionDefinition = objectMapper
            .readValue(fileContent, JurisdictionDefinition.class);
        JurisdictionDefinition copiedJurisdictionDefinition  = jurisdictionDefinition.createCopy();

        assertNotEquals(jurisdictionDefinition.hashCode(), copiedJurisdictionDefinition.hashCode());

        String originalJson = objectMapper.writeValueAsString(jurisdictionDefinition);
        String copiedJson = objectMapper.writeValueAsString(copiedJurisdictionDefinition);

        String originalJsonHash256 = DigestUtils.sha256Hex(originalJson);
        String copiedJsonHash256 = DigestUtils.sha256Hex(copiedJson);

        assertNotEquals(jurisdictionDefinition.hashCode(), copiedJurisdictionDefinition.hashCode());
        assertEquals(originalJsonHash256, copiedJsonHash256);
    }

}
