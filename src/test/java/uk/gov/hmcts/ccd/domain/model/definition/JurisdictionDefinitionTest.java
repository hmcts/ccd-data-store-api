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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static uk.gov.hmcts.ccd.TestFixtures.fromFileAsString;

class JurisdictionDefinitionTest {
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
    public void autoTest1JurisdictionCompareActualAndClone() throws JsonProcessingException {
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
    public void beftaMasterJurisdictionCompareActualAndClone() throws JsonProcessingException {
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
    public void beftaJurisdiction3JurisdictionCompareActualAndClone() throws JsonProcessingException {
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
