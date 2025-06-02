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
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.PseudoRoleToAccessProfileGenerator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static uk.gov.hmcts.ccd.TestFixtures.fromFileAsString;

class RoleToAccessProfileDefinitionTest {

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
    public void ftMasterCaseTypeRoleToAccessProfileDefinitionCompareActualAndClone() throws JsonProcessingException {
        String fileContent = fromFileAsString("tests/FT-MasterCaseType-payload.json");

        CaseTypeDefinition caseTypeDefinition = objectMapper.readValue(fileContent, CaseTypeDefinition.class);
        PseudoRoleToAccessProfileGenerator accessProfileGenerator = new PseudoRoleToAccessProfileGenerator();
        List<RoleToAccessProfileDefinition> accessProfiles = accessProfileGenerator.generate(caseTypeDefinition);
        List<RoleToAccessProfileDefinition> copiedAccessProfiles =
            accessProfiles.stream().map(RoleToAccessProfileDefinition::createCopy).toList();

        for (int i = 0; i < accessProfiles.size(); i++) {
            RoleToAccessProfileDefinition accessProfile = accessProfiles.get(i);
            RoleToAccessProfileDefinition copiedAccessProfile = copiedAccessProfiles.get(i);

            String originalJson = objectMapper.writeValueAsString(accessProfile);
            String copiedJson = objectMapper.writeValueAsString(copiedAccessProfile);

            String originalJsonHash256 = DigestUtils.sha256Hex(originalJson);
            String copiedJsonHash256 = DigestUtils.sha256Hex(copiedJson);

            assertAll(
                () -> assertNotSame(accessProfile, copiedAccessProfile),
                () -> assertEquals(originalJsonHash256, copiedJsonHash256)
            );
        }
    }

    @Test
    public void ftComplexCrudRoleToAccessProfileDefinitionCompareActualAndClone() throws JsonProcessingException {
        String fileContent = fromFileAsString("tests/FT-ComplexCRUD-payload.json");

        CaseTypeDefinition caseTypeDefinition = objectMapper.readValue(fileContent, CaseTypeDefinition.class);
        PseudoRoleToAccessProfileGenerator accessProfileGenerator = new PseudoRoleToAccessProfileGenerator();
        List<RoleToAccessProfileDefinition> accessProfiles = accessProfileGenerator.generate(caseTypeDefinition);
        List<RoleToAccessProfileDefinition> copiedAccessProfiles =
            accessProfiles.stream().map(RoleToAccessProfileDefinition::createCopy).toList();

        for (int i = 0; i < accessProfiles.size(); i++) {
            RoleToAccessProfileDefinition accessProfile = accessProfiles.get(i);
            RoleToAccessProfileDefinition copiedAccessProfile = copiedAccessProfiles.get(i);

            String originalJson = objectMapper.writeValueAsString(accessProfile);
            String copiedJson = objectMapper.writeValueAsString(copiedAccessProfile);

            String originalJsonHash256 = DigestUtils.sha256Hex(originalJson);
            String copiedJsonHash256 = DigestUtils.sha256Hex(copiedJson);

            assertAll(
                () -> assertNotSame(accessProfile, copiedAccessProfile),
                () -> assertEquals(originalJsonHash256, copiedJsonHash256)
            );
        }
    }

    @Test
    public void beftaCaseType31RoleToAccessProfileDefinitionCompareActualAndClone() throws JsonProcessingException {
        String fileContent = fromFileAsString("tests/BEFTA-CASETYPE-3-1-payload.json");

        CaseTypeDefinition caseTypeDefinition = objectMapper.readValue(fileContent, CaseTypeDefinition.class);
        PseudoRoleToAccessProfileGenerator accessProfileGenerator = new PseudoRoleToAccessProfileGenerator();
        List<RoleToAccessProfileDefinition> accessProfiles = accessProfileGenerator.generate(caseTypeDefinition);
        List<RoleToAccessProfileDefinition> copiedAccessProfiles =
            accessProfiles.stream().map(RoleToAccessProfileDefinition::createCopy).toList();

        for (int i = 0; i < accessProfiles.size(); i++) {
            RoleToAccessProfileDefinition accessProfile = accessProfiles.get(i);
            RoleToAccessProfileDefinition copiedAccessProfile = copiedAccessProfiles.get(i);

            String originalJson = objectMapper.writeValueAsString(accessProfile);
            String copiedJson = objectMapper.writeValueAsString(copiedAccessProfile);

            String originalJsonHash256 = DigestUtils.sha256Hex(originalJson);
            String copiedJsonHash256 = DigestUtils.sha256Hex(copiedJson);

            assertAll(
                () -> assertNotSame(accessProfile, copiedAccessProfile),
                () -> assertEquals(originalJsonHash256, copiedJsonHash256)
            );
        }
    }
}
