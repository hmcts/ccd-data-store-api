package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.config.JacksonObjectMapperConfig;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseAccessGroup;
import uk.gov.hmcts.ccd.domain.model.definition.CaseAccessGroupWithId;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.AccessTypeRoleDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static uk.gov.hmcts.ccd.config.JacksonUtils.MAPPER;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.TEXT;

@ExtendWith(MockitoExtension.class)
class CaseAccessGroupUtilsTest {

    @Spy
    private ObjectMapper objectMapper = new JacksonObjectMapperConfig().defaultObjectMapper();
    @Captor
    private ArgumentCaptor<Map<String, JsonNode>> caseDataCaptor;
    private final Map<String, JsonNode> caseData = new HashMap<>();
    @Mock
    private CaseDataService caseDataService;
    private CaseTypeDefinition caseTypeDefinition;
    private CaseAccessGroupUtils caseAccessGroupUtils;
    public static final String TEST_SOMETHING_ELSE = "some thing else";
    public static final String TEST_ORGID = "550e8400-e29b-41d4-a716-446655440000";

    @Nested
    @DisplayName("updateCaseDataWithCaseAccessGroups")
    class UpdateCaseDataWithCaseAccessGroups {

        @BeforeEach
        void setup() {
            caseAccessGroupUtils = new CaseAccessGroupUtils(caseDataService, objectMapper);
            caseTypeDefinition = createCaseTypeDefinitionWithCaseAccessGroup();
        }

        @Test
        void updateCaseDataWithCaseAccessGroups_WithCcdAllCasesAndSomething() {

            // GIVEN
            var caseDetails = setupEverythingForTest(caseTypeDefinition);
            mockCaseDetails(caseDetails, caseTypeDefinition);

            Map<String, JsonNode> expectedCaseData = getClonedCaseAccessGroups(caseDetails.getData());
            Map<String, JsonNode> expectedCaseDataClassification =
                getClonedCaseAccessGroups(caseDetails.getDataClassification());

            // WHEN
            caseAccessGroupUtils.updateCaseAccessGroupsInCaseDetails(caseDetails,  caseTypeDefinition);

            // THEN
            verifyGetDefaultSecurityClassificationsCall(expectedCaseDataClassification, caseTypeDefinition, 2);
            assertNotEquals(expectedCaseData.get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS).size(),
                caseDetails.getData().get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS).size());
            assertEquals(caseDetails.getData().get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS).size(), 3);
            assertTrue(caseDetails.getData().containsKey(CaseAccessGroupUtils.CASE_ACCESS_GROUPS));
            assertTrue(caseDetails.getData().get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS)
                .toString().contains(TEST_SOMETHING_ELSE));
            assertTrue(caseDetails.getData().get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS)
                .toString().contains(TEST_ORGID));

        }

        @Test
        void updateCaseDataWithCaseAccessGroups_WithNoOrgCcdAllCasesAndSomething() {

            // GIVEN
            var caseDetails = setupEverythingNoOrgForTest(caseTypeDefinition);
            mockCaseDetails(caseDetails, caseTypeDefinition);

            Map<String, JsonNode> expectedCaseDataClassification =
                getClonedCaseAccessGroups(caseDetails.getDataClassification());

            // WHEN
            Map<String, JsonNode> expectedCaseData = getClonedCaseAccessGroups(caseDetails.getData());
            caseAccessGroupUtils.updateCaseAccessGroupsInCaseDetails(caseDetails,  caseTypeDefinition);

            // THEN
            verifyGetDefaultSecurityClassificationsCall(expectedCaseDataClassification, caseTypeDefinition,2);
            assertNotEquals(expectedCaseData.get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS).size(),
                caseDetails.getData().get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS).size());
            assertEquals(caseDetails.getData().get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS).size(), 1);
            assertTrue(caseDetails.getData().containsKey(CaseAccessGroupUtils.CASE_ACCESS_GROUPS));
            assertTrue(caseDetails.getData().get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS)
                .toString().contains(TEST_SOMETHING_ELSE));
            assertFalse(caseDetails.getData().get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS)
                .toString().contains(TEST_ORGID));

        }

        @Test
        void updateCaseDataWithCaseAccessGroups_WithNoOrgCcdAllCases() {

            // GIVEN
            var caseDetails = setupNoOrgCcdAllCasesForTest(caseTypeDefinition);
            mockCaseDetails(caseDetails, caseTypeDefinition);

            Map<String, JsonNode> expectedCaseDataClassification =
                getClonedCaseAccessGroups(caseDetails.getDataClassification());

            // WHEN
            caseAccessGroupUtils.updateCaseAccessGroupsInCaseDetails(caseDetails,  caseTypeDefinition);

            // THEN
            verifyGetDefaultSecurityClassificationsCall(expectedCaseDataClassification, caseTypeDefinition,1);
            assertNull(caseDetails.getData().get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS));
            assertFalse(caseDetails.getData().containsKey(CaseAccessGroupUtils.CASE_ACCESS_GROUPS));
        }

        @Test
        void updateCaseDataWithCaseAccessGroups_WithSomething() {

            // GIVEN
            var caseDetails = setupCaseAccessGroupSomething(caseTypeDefinition);
            mockCaseDetails(caseDetails, caseTypeDefinition);

            Map<String, JsonNode> expectedCaseData = getClonedCaseAccessGroups(caseDetails.getData());
            Map<String, JsonNode> expectedCaseDataClassification =
                getClonedCaseAccessGroups(caseDetails.getDataClassification());

            // WHEN
            caseAccessGroupUtils.updateCaseAccessGroupsInCaseDetails(caseDetails,  caseTypeDefinition);

            // THEN
            verifyGetDefaultSecurityClassificationsCall(expectedCaseDataClassification, caseTypeDefinition, 2);
            assertNotEquals(expectedCaseData.get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS).size(),
                caseDetails.getData().get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS).size());
            // 1 - SOMETHING ELSE
            // +2 CCD:all-cases-access in AccessTypeRole (CaseAccessGroupUtils.CCD_ALL_CASES)
            assertEquals(caseDetails.getData().get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS).size(), 3);
            assertTrue(caseDetails.getData().containsKey(CaseAccessGroupUtils.CASE_ACCESS_GROUPS));
            assertTrue(caseDetails.getData().get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS)
                .toString().contains(TEST_SOMETHING_ELSE));
            assertTrue(caseDetails.getData().get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS)
                .toString().contains(TEST_ORGID));

        }

        @Test
        void updateCaseDataWithCaseAccessGroups_WithSomethingOnly() {

            // GIVEN
            var caseDetails = setupCaseAccessGroupSomethingOnly(caseTypeDefinition);
            mockCaseDetails(caseDetails, caseTypeDefinition);

            Map<String, JsonNode> expectedCaseData = getClonedCaseAccessGroups(caseDetails.getData());
            Map<String, JsonNode> expectedCaseDataClassification =
                getClonedCaseAccessGroups(caseDetails.getDataClassification());

            // WHEN
            caseAccessGroupUtils.updateCaseAccessGroupsInCaseDetails(caseDetails,  caseTypeDefinition);

            // THEN
            verifyGetDefaultSecurityClassificationsCall(expectedCaseDataClassification, caseTypeDefinition, 2);
            assertNotEquals(expectedCaseData.get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS).size(),
                caseDetails.getData().get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS).size());
            // 1 - SOMETHING ELSE
            // 0 CCD:all-cases-access in AccessTypeRole (CaseAccessGroupUtils.CCD_ALL_CASES)
            assertEquals(caseDetails.getData().get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS).size(), 1);
            assertTrue(caseDetails.getData().containsKey(CaseAccessGroupUtils.CASE_ACCESS_GROUPS));
            assertTrue(caseDetails.getData().get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS)
                .toString().contains(TEST_SOMETHING_ELSE));
            assertFalse(caseDetails.getData().get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS)
                .toString().contains(TEST_ORGID));

        }

        @Test
        void updateCaseDataWithCaseAccessGroups_WithNothing() {

            // GIVEN
            var caseDetails = setUpCaseDetails();

            // WHEN
            caseAccessGroupUtils.updateCaseAccessGroupsInCaseDetails(caseDetails,  caseTypeDefinition);

            // THEN
            assertNull(caseDetails.getData().get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS));
        }
    }

    @Nested
    @DisplayName("updateCaseDataClassificationWithCaseAccessGroups")
    class UpdateCaseDataClassificationWithCaseAccessGroups {

        @BeforeEach
        void setup() {
            caseAccessGroupUtils = new CaseAccessGroupUtils(caseDataService, objectMapper);
            caseTypeDefinition = createCaseTypeDefinitionWithCaseAccessGroup();
        }

        @Test
        void updateCaseDataClassificationWithCaseAccessGroups_Everthing() {

            // GIVEN
            var caseDetails = setupEverythingForTest(caseTypeDefinition);

            mockCaseDetails(caseDetails, caseTypeDefinition);

            Map<String, JsonNode> expectedCaseDataClassification =
                getClonedCaseAccessGroups(caseDetails.getDataClassification());

            // WHEN
            var output = caseAccessGroupUtils.updateCaseDataClassificationWithCaseGroupAccess(caseDetails,
                caseTypeDefinition);

            // THEN
            assertEquals(caseDetails.getDataClassification(), output);
            assertEquals(expectedCaseDataClassification, caseDetails.getDataClassification());
            assertEquals(expectedCaseDataClassification.get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS).size(),
                caseDetails.getDataClassification().get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS).size());
            assertTrue(caseDetails.getDataClassification().containsKey(CaseAccessGroupUtils.CASE_ACCESS_GROUPS));
        }

        @Test
        void updateCaseDataClassificationWithCaseAccessGroups_unchangedWhenCaseDataMissingCaseAccessGroups() {

            // GIVEN
            var caseDetails = setupOrganisation(caseTypeDefinition);

            // WHEN
            var output = caseAccessGroupUtils.updateCaseDataClassificationWithCaseGroupAccess(
                caseDetails, caseTypeDefinition);

            // THEN
            assertEquals(caseDetails.getDataClassification(), output);
        }

    }

    private CaseDetails setupOrganisation(CaseTypeDefinition caseTypeDefinition) {
        CaseDetails caseDetails = setUpCaseDetails();
        setUpAccessTypeRoles(caseTypeDefinition);

        addOrganisationPolicyToCaseDetailsData(caseDetails);
        return caseDetails;
    }

    private CaseDetails setupCaseAccessGroupSomething(CaseTypeDefinition caseTypeDefinition) {
        CaseDetails caseDetails = setUpCaseDetails();
        setUpAccessTypeRoles(caseTypeDefinition);

        addOrganisationPolicyToCaseDetailsData(caseDetails);
        addCaseAccessGroupsToCaseDetailsDataWithSomething(caseDetails);
        return caseDetails;
    }

    private CaseDetails setupCaseAccessGroupSomethingOnly(CaseTypeDefinition caseTypeDefinition) {
        CaseDetails caseDetails = setUpCaseDetails();
        setUpAccessTypeRoles(caseTypeDefinition);

        addCaseAccessGroupsToCaseDetailsDataWithSomething(caseDetails);
        return caseDetails;
    }

    private CaseDetails setupEverythingForTest(CaseTypeDefinition caseTypeDefinition) {
        CaseDetails caseDetails = setUpCaseDetails();
        setUpAccessTypeRoles(caseTypeDefinition);

        addOrganisationPolicyToCaseDetailsData(caseDetails);
        addCaseAccessGroupsToCaseDetailsDataWithCCD_All_Cases(caseDetails);
        addCaseAccessGroupsToCaseDetailsDataWithSomething(caseDetails);
        return caseDetails;
    }

    private CaseDetails setupNoOrgCcdAllCasesForTest(CaseTypeDefinition caseTypeDefinition) {
        CaseDetails caseDetails = setUpCaseDetails();
        setUpAccessTypeRoles(caseTypeDefinition);

        addCaseAccessGroupsToCaseDetailsDataWithCCD_All_Cases(caseDetails);
        return caseDetails;
    }

    private CaseDetails setupEverythingNoOrgForTest(CaseTypeDefinition caseTypeDefinition) {
        CaseDetails caseDetails = setUpCaseDetails();
        setUpAccessTypeRoles(caseTypeDefinition);

        addCaseAccessGroupsToCaseDetailsDataWithCCD_All_Cases(caseDetails);
        addCaseAccessGroupsToCaseDetailsDataWithSomething(caseDetails);
        return caseDetails;
    }

    private CaseDetails setUpCaseDetails() {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId("SomeCaseType");
        caseDetails.setJurisdiction("SomeJurisdiction");
        caseDetails.setState("SomeState");
        caseDetails.setData(caseData);
        caseDetails.setSecurityClassification(SecurityClassification.PUBLIC);
        return caseDetails;
    }

    private void setUpAccessTypeRoles(CaseTypeDefinition caseTypeDefinition) {

        AccessTypeRoleDefinition accessTypeRolesDefinition = new AccessTypeRoleDefinition();
        accessTypeRolesDefinition.setCaseTypeId(caseTypeDefinition.getId());
        accessTypeRolesDefinition.setAccessTypeId("someAccessTypeId");
        accessTypeRolesDefinition.setOrganisationalRoleName("someOrgProfileName");
        accessTypeRolesDefinition.setGroupRoleName("GroupRoleName");
        accessTypeRolesDefinition.setCaseAccessGroupIdTemplate("SomeJurisdiction:CIVIL:bulk:"
            + "[RESPONDENT01SOLICITOR]:$ORGID$");
        accessTypeRolesDefinition.setCaseAssignedRoleField("caseAssignedField");

        List<AccessTypeRoleDefinition> accessTypeRolesDefinitions = new ArrayList<AccessTypeRoleDefinition>();
        accessTypeRolesDefinitions.add(accessTypeRolesDefinition);

        accessTypeRolesDefinition.setCaseTypeId(caseTypeDefinition.getId());
        accessTypeRolesDefinition.setAccessTypeId("someAccessTypeId1");
        accessTypeRolesDefinition.setOrganisationalRoleName("someOrgProfileName1");

        AccessTypeRoleDefinition accessTypeRolesDefinition1 = new AccessTypeRoleDefinition();
        accessTypeRolesDefinition1.setGroupRoleName("GroupRoleName1");
        accessTypeRolesDefinition1.setCaseAccessGroupIdTemplate("SomeJurisdictionCIVIL:bulk:"
            + "[RESPONDENT01SOLICITOR]:$ORGID$");
        accessTypeRolesDefinition1.setCaseAssignedRoleField("caseAssignedField");
        accessTypeRolesDefinitions.add(accessTypeRolesDefinition1);

        caseTypeDefinition.setAccessTypeRoleDefinitions(accessTypeRolesDefinitions);

    }

    private void addCaseAccessGroupsToCaseDetailsDataWithSomething(CaseDetails caseDetails) {
        String caseAccessGroupType = TEST_SOMETHING_ELSE;
        String caseAccessGroupID = "SomeJurisdiction:CIVIL:bulk: [RESPONDENT01SOLICITOR]:"
            + "Any Value";
        Map<String, JsonNode> dataCaseAccessGroup = caseAccessGroupCaseData(caseAccessGroupType,
            caseAccessGroupID);

        JsonNode caseAccessGroupNode = objectMapper
            .convertValue(dataCaseAccessGroup.get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS), JsonNode.class);
        mergeCaseAccessGroupsWithNew(caseDetails, caseAccessGroupNode);

    }

    private void mergeCaseAccessGroupsWithNew(CaseDetails caseDetails, JsonNode newCaseAccessGroup) {
        JsonNode caseDataCaseAccessGroup = caseDetails.getData().get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS);

        String mergedValue = null;
        if (caseDataCaseAccessGroup != null && !caseDataCaseAccessGroup.isEmpty()) {
            mergedValue = caseDataCaseAccessGroup + newCaseAccessGroup.toString();
            mergedValue = mergedValue.replace("][",",");
        } else {
            mergedValue = newCaseAccessGroup.toString();
        }

        JsonNode mergedNode = null;
        try {
            mergedNode = new ObjectMapper().readTree(mergedValue);
        } catch (JsonProcessingException e) {
            throw new ValidationException(String.format(e.getMessage()));
        }

        caseDetails.getData().put(CaseAccessGroupUtils.CASE_ACCESS_GROUPS, mergedNode);
    }

    private void addCaseAccessGroupsToCaseDetailsDataWithCCD_All_Cases(CaseDetails caseDetails) {
        String caseAccessGroupType = "CCD:all-cases-access";
        String caseAccessGroupID = "SomeJurisdiction:CIVIL:bulk: [RESPONDENT01SOLICITOR]:"
            + "Any Value";
        Map<String, JsonNode> dataCaseAccessGroup = caseAccessGroupCaseData(caseAccessGroupType,
            caseAccessGroupID);

        JacksonUtils.merge(JacksonUtils.convertValue(dataCaseAccessGroup), caseDetails.getData());

    }

    private void addOrganisationPolicyToCaseDetailsData(CaseDetails caseDetails) {
        Map<String, JsonNode> dataOrganisation = null;
        try {
            dataOrganisation = organisationPolicyCaseData("caseAssignedField",
                "\"" + TEST_ORGID + "\"");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        JacksonUtils.merge(JacksonUtils.convertValue(dataOrganisation), caseDetails.getData());
    }

    private Map<String, JsonNode> organisationPolicyCaseData(String role, String organisationId)
        throws JsonProcessingException {

        JsonNode data = MAPPER.readTree(""
            + "{"
            + "  \"Organisation\": {"
            + "    \"OrganisationID\": " + organisationId + ","
            + "    \"OrganisationName\": \"OrganisationName1\""
            + "  },"
            + "  \"OrgPolicyReference\": null,"
            + "  \"OrgPolicyCaseAssignedRole\": \"" + role + "\""
            + "}");

        Map<String, JsonNode> result = new HashMap<>();
        result.put("OrganisationPolicyField", data);
        return result;
    }

    private Map<String, JsonNode> caseAccessGroupCaseData(String caseAccessGroupType, String caseAccessGroupID) {

        CaseAccessGroup caseAccessGroup = CaseAccessGroup.builder().caseAccessGroupId(caseAccessGroupID)
            .caseAccessGroupType(caseAccessGroupType).build();

        CaseAccessGroupWithId caseAccessGroupForUI = CaseAccessGroupWithId.builder()
            .caseAccessGroup(caseAccessGroup)
            .id(UUID.randomUUID().toString()).build();

        List<CaseAccessGroupWithId> caseAccessGroupForUIs = new ArrayList<>();
        caseAccessGroupForUIs.add(caseAccessGroupForUI);

        CaseAccessGroup caseAccessGroup1 = CaseAccessGroup.builder()
            .caseAccessGroupId("SomeJurisdictionCIVIL:bulk: [RESPONDENT02SOLICITOR]:$ORG$")
            .caseAccessGroupType("CCD:all-cases-access").build();

        CaseAccessGroupWithId caseAccessGroupForUI1 = CaseAccessGroupWithId.builder()
            .caseAccessGroup(caseAccessGroup1)
            .id(UUID.randomUUID().toString()).build();

        caseAccessGroupForUIs.add(caseAccessGroupForUI1);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode data  = mapper.convertValue(caseAccessGroupForUIs, JsonNode.class);
        Map<String, JsonNode> result = new HashMap<>();
        result.put(CaseAccessGroupUtils.CASE_ACCESS_GROUPS, data);
        return result;
    }

    private CaseDetails mockCaseDetails(CaseDetails caseDetails, CaseTypeDefinition caseTypeDefinition) {
        mockGetDefaultSecurityClassificationsResponse(caseDetails);

        Map<String, JsonNode> caseDataClassificationWithCaseAccessGroup =
            caseAccessGroupUtils.updateCaseDataClassificationWithCaseGroupAccess(
                caseDetails, caseTypeDefinition);

        caseDetails.setDataClassification(caseDataClassificationWithCaseAccessGroup);

        return caseDetails;
    }

    private void verifyGetDefaultSecurityClassificationsCall(Map<String, JsonNode> data,
                                                             CaseTypeDefinition caseTypeDefinition, int noOfTimes) {

        verify(caseDataService, times(noOfTimes)).getDefaultSecurityClassifications(
            eq(caseTypeDefinition),
            caseDataCaptor.capture(),
            any()
        );
        // must have CaseAccessGroups value
        assertNotEquals(data.get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS),
            caseDataCaptor.getValue().get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS));
    }

    private CaseTypeDefinition createCaseTypeDefinitionWithoutCaseAccessGroup() {

        CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();
        caseFieldDefinition.setId("myTextField");
        caseFieldDefinition.setFieldTypeDefinition(createFieldTypeDefinition(TEXT));

        List<CaseFieldDefinition> caseFields = new ArrayList<>();
        caseFields.add(caseFieldDefinition);

        var newCaseTypeDefinition = new CaseTypeDefinition();
        newCaseTypeDefinition.setCaseFieldDefinitions(caseFields);

        return newCaseTypeDefinition;
    }

    private CaseTypeDefinition createCaseTypeDefinitionWithCaseAccessGroup() {
        var newCaseTypeDefinition = createCaseTypeDefinitionWithoutCaseAccessGroup();

        CaseFieldDefinition caseAccessGroupsFieldDefinition = new CaseFieldDefinition();
        caseAccessGroupsFieldDefinition.setId(CaseAccessGroupUtils.CASE_ACCESS_GROUPS);
        caseAccessGroupsFieldDefinition
            .setFieldTypeDefinition(createFieldTypeDefinition(CaseAccessGroupUtils.CASE_ACCESS_GROUPS));

        newCaseTypeDefinition.getCaseFieldDefinitions().add(caseAccessGroupsFieldDefinition);
        return newCaseTypeDefinition;
    }

    private FieldTypeDefinition createFieldTypeDefinition(String type) {
        FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
        fieldTypeDefinition.setId(type);
        fieldTypeDefinition.setType(type);
        return fieldTypeDefinition;
    }

    private Map<String, JsonNode> getClonedCaseAccessGroups(Map<String, JsonNode> data) {
        Map<String, JsonNode> clonedData = new HashMap<>(data);

        if (data.containsKey(CaseAccessGroupUtils.CASE_ACCESS_GROUPS)) {
            var expectedCaseAccessGroupsJson = data.get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS).deepCopy();
            clonedData.put(CaseAccessGroupUtils.CASE_ACCESS_GROUPS, expectedCaseAccessGroupsJson);
        }

        return clonedData;
    }

    private void mockGetDefaultSecurityClassificationsResponse(CaseDetails caseDetails) {
        JsonNode caseAccessGroupJsonNode = caseDetails.getData().get(CaseAccessGroupUtils.CASE_ACCESS_GROUPS);
        Map<String, JsonNode> dataClassification = new HashMap<>();
        dataClassification.put(CaseAccessGroupUtils.CASE_ACCESS_GROUPS, caseAccessGroupJsonNode);

        doReturn(dataClassification).when(caseDataService).getDefaultSecurityClassifications(
            any(), any(), any()
        );
    }

}
