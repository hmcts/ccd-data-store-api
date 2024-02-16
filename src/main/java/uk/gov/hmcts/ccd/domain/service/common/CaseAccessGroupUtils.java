package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseAccessGroup;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.AccessTypeRolesDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.VALUE;

public class CaseAccessGroupUtils {

    public static final String CASE_ACCESS_GROUPS = "caseAccessGroups";

    protected static final String FAILED_TO_READ_CASE_ACCESS_GROUPS_FROM_CASE_DATA =
        "Failed to read 'caseAccessGroups' from case data";
    protected static final String CCD_ALL_CASES = "CCD:all-cases-access";
    protected static final String ORGANISATIONID = "Organisation.OrganisationID";

    private CaseTypeDefinition caseTypeDefinition;

    public CaseAccessGroupUtils() {

    }

    public void updateCaseAccessGroupsInCaseDetails(CaseDetails caseDetails, CaseTypeDefinition caseTypeDefinition) {

        this.caseTypeDefinition = caseTypeDefinition;
        /*
        1. Remove all items from the case data caseAccessGroups collection where caseGroupType = CCD:all-cases-access.
        */
        CaseDetails removedCaseDetails = removeCCDAllCasesAccessFromCaseAccessGroups(caseDetails);
        /*
        2. Filter all AccessTypeRole records (joined with their owning AccessType records), keeping records where:
            . GroupRoleName is not empty.
            . CaseAssignedRoleField has a value, and the field exists in the case data.
              (Note that this may be a field of type OrganisationPolicy, or a complex type field with equivalent
              structure.)
            . For the field retrieved from the case data, Organisation.OrganisationID has a non-empty value.
        */

        List<AccessTypeRolesDefinition> accessTypeRolesDefinitions = caseTypeDefinition.getAccessTypeRolesDefinitions();
        List<AccessTypeRolesDefinition>  filteredAccessTypeRolesDefinitions = filterAccessRoles(caseDetails,
            accessTypeRolesDefinitions);
        /*
        3. For each retained record, create a case group ID value by substituting the organisation policy
           Organisation.OrganisationID value in CaseTypeOrganisationAccess.CaseGroupIDTemplate. For example :
           . CaseGroupIDTemplate is set to - CIVIL:bulk:[RESPONDENT01SOLICITOR]:$ORGID$
           . Organisation.OrganisationID value set in organisation policy - 550e8400-e29b-41d4-a716-446655440000
           . Substituting Organisation.OrganisationID value in CaseGroupIDTemplate to have a new case group ID
              - CIVIL:bulk:[RESPONDENT01SOLICITOR]:550e8400-e29b-41d4-a716-446655440000
        */
        if (!caseDetails.getData().isEmpty()) {
            Map<String, JsonNode> caseDatawithOrganisationID =
                (Map<String, JsonNode>) caseDetails.getData().entrySet().stream()
                    .filter(cd -> cd.getKey().equals(ORGANISATIONID)
                        && StringUtils.isNoneBlank(cd.getValue().toString()))
                    .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue,
                        (x,y) -> y,
                        HashMap::new));

            JsonNode organisationIDdata = JacksonUtils.convertValueJsonNode(caseDatawithOrganisationID);
            final Iterator<String> fieldNames = organisationIDdata.fieldNames();

            List<CaseAccessGroup> caseAccessGroups = buildGroupAccessId(organisationIDdata,
                filteredAccessTypeRolesDefinitions);

            /*
            4. Add each case group ID to the caseAccessGroups collection in the case data, setting
           caseGroupType = CCD:all-cases-access
            */
            if (caseAccessGroups != null && !caseAccessGroups.isEmpty()) {
                for (CaseAccessGroup cag : caseAccessGroups) {
                    cag.setCaseAccessGroupType(CCD_ALL_CASES);
                    JsonNode caseAccessGroupNode = JacksonUtils.convertValueJsonNode(cag);

                    //Add caseAccessGroups to the caseAccessGroups collection in the case data
                    JacksonUtils.merge(JacksonUtils.convertValue(caseAccessGroupNode), caseDetails.getData());
                }
            }
        }
    }

    private CaseDetails removeCCDAllCasesAccessFromCaseAccessGroups(CaseDetails currentCaseDetails) {
        /*
        1. Remove all items from the case data caseAccessGroups collection where caseGroupType = CCD:all-cases-access.
        */
        CaseDetails results = currentCaseDetails;
        List<CaseAccessGroup> caseAccessGroups = getCaseAccessGroupFromCaseData(currentCaseDetails.getData());
        results.getData().entrySet().removeIf(caseDetails -> isCCDAllCasesAccess(caseAccessGroups, currentCaseDetails));
        return (CaseDetails) results;
    }

    private List<CaseAccessGroup> getCaseAccessGroupFromCaseData(Map<String, JsonNode> caseData) {

        JsonNode caseAccessGroupJsonNode = caseData.get(CASE_ACCESS_GROUPS);
        if (caseAccessGroupJsonNode != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                return Collections.singletonList(objectMapper.readValue(caseAccessGroupJsonNode.toString(),
                    CaseAccessGroup.class));
            } catch (JsonProcessingException e) {
                throw new ValidationException(FAILED_TO_READ_CASE_ACCESS_GROUPS_FROM_CASE_DATA);
            }
        }
        return null;
    }

    private boolean isCCDAllCasesAccess(List<CaseAccessGroup> caseAccessGroups, CaseDetails caseDetails) {
        boolean isCCDAllCasesAccess = false;
        if (caseAccessGroups != null && !caseAccessGroups.isEmpty()) {
            for (CaseAccessGroup caseAccessGroup : caseAccessGroups) {
                if (caseAccessGroup.getCaseAccessGroupType().equals(CCD_ALL_CASES)) {
                    isCCDAllCasesAccess = true;
                    break;
                }
            }
        }
        return isCCDAllCasesAccess;
    }

    private List<AccessTypeRolesDefinition> filterAccessRoles(CaseDetails caseDetails,
                                   List<AccessTypeRolesDefinition> accessTypeRolesDefinitions) {
        /*
        2. Filter all AccessTypeRole records (joined with their owning AccessType records), keeping records where:
            . GroupRoleName is not empty.
            . CaseAssignedRoleField has a value, and the field exists in the case data.
              (Note that this may be a field of type OrganisationPolicy, or a complex type field with equivalent
              structure.)
            . For the field retrieved from the case data, Organisation.OrganisationID has a non-empty value.
        */
        CaseDetails caseDetailsWithCaseAssignedRoleField;

        List<AccessTypeRolesDefinition> accessTypeRolesDefinitionWithCaseAssignedRoleField =
            accessTypeRolesDefinitions.stream()
                /*GroupRoleName is not empty.
                CaseAssignedRoleField has a value, and the field exists in the case data.
                (Note that this may be a field of type OrganisationPolicy, or a complex type field with equivalent
                    structure.)
                */
                .filter(accessTypeRole ->
                    StringUtils.isNoneBlank(accessTypeRole.getGroupRoleName())
                        && StringUtils.isNoneBlank(accessTypeRole.getCaseAssignedRoleField())
                        && caseDetails.getData().entrySet().contains(accessTypeRole.getCaseAssignedRoleField()))
                .collect(Collectors.toList());

        //. For the field retrieved from the case data, Organisation.OrganisationID has a non-empty value.
        List<AccessTypeRolesDefinition> accessTypeRolesDefinitionWithNonEmptyOrganisationID =
            accessTypeRolesDefinitionWithCaseAssignedRoleField.stream()
                .filter(element ->  isExitsCaseDetailsWithCaseAssignedRoleFieldOrganisationID(caseDetails,
                    element.getCaseAssignedRoleField()))
                .collect(Collectors.toList());
        return accessTypeRolesDefinitionWithNonEmptyOrganisationID;
    }



    private Boolean isExitsCaseDetailsWithCaseAssignedRoleFieldOrganisationID(CaseDetails caseDetails,
                                                                              String caseAssignedRoleField) {
        Map<String, JsonNode> caseAccessGroupJsonNode = caseDetails.getData();
        //part of 2. For the field retrieved from the case data,  Organisation.OrganisationID has a non-empty value.

        if (caseDetails.getData().entrySet().contains(caseAssignedRoleField)) {
            Map<String, JsonNode> withcaseAssignedRoleField =
                (Map<String, JsonNode>) caseAccessGroupJsonNode.entrySet().stream()
                .filter(cd -> cd.getKey().equals(caseAssignedRoleField)
                    && StringUtils.isNoneBlank(cd.getValue().toString()))
                .collect(Collectors.toMap(Map.Entry::getKey,
                    Map.Entry::getValue,
                    (x,y) -> y,
                    HashMap::new));

            Map<String, JsonNode> withCaseOrganisationID =
                (Map<String, JsonNode>) withcaseAssignedRoleField.entrySet().stream()
                .filter(cd -> cd.getKey().equals(ORGANISATIONID)
                    && StringUtils.isNoneBlank(cd.getValue().toString()))
                .collect(Collectors.toMap(Map.Entry::getKey,
                    Map.Entry::getValue,
                    (x,y) -> y,
                    HashMap::new));


            if (withCaseOrganisationID != null && !withCaseOrganisationID.isEmpty()) {
                //caseDetails has Organisation.OrganisationID and is a non-empty value
                return true;
            }
        }
        return false;
    }

    private List<CaseAccessGroup> buildGroupAccessId(
        final JsonNode organisationDataNode,
        List<AccessTypeRolesDefinition> filteredAccessTypeRolesDefinitions) {

        List<CaseAccessGroup> caseAccessGroups = null;
        for (AccessTypeRolesDefinition accessTypeRolesDefinition : filteredAccessTypeRolesDefinitions) {
            if (caseAccessGroups == null) {
                caseAccessGroups = new ArrayList<>();
            }

            List<CaseFieldDefinition> caseFieldDefinitions = caseTypeDefinition.getCaseFieldDefinitions();
            final Iterator<String> fieldNames = organisationDataNode.fieldNames();
            while (fieldNames.hasNext()) {
                final String fieldName = fieldNames.next();
                Iterator<CaseFieldDefinition> cfIterator = caseFieldDefinitions.iterator();
                while (cfIterator.hasNext()) {
                    organisationDataNode.forEach(caseFieldValueJsonNode -> {
                        if (caseFieldValueJsonNode.get(VALUE).get(fieldName) != null) {
                            // Build the Group Access
                            String caseGroupID = accessTypeRolesDefinition.getCaseAccessGroupIdTemplate()
                                .replace("$ORGID$", caseFieldValueJsonNode.get(VALUE).toString());
                        }
                    });
                }
            }
        }
        return caseAccessGroups;
    }
}
