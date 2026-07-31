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
        objectMapper = JsonMapper.builderWithJackson2Defaults()
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build();
    }

    @Test
    public void ftMasterCaseTypeRoleToAccessProfileDefinitionCompareActualAndClone() throws JacksonException {
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
    public void ftComplexCrudRoleToAccessProfileDefinitionCompareActualAndClone() throws JacksonException {
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
    public void beftaCaseType31RoleToAccessProfileDefinitionCompareActualAndClone() throws JacksonException {
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
