package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CaseDataAccessTypeRolesUtils {

    protected static final String FAILED_TO_READ_CASE_ACCESS_GROUPS_FROM_CASE_DATA = "Failed to read 'caseAccessGroups' from case data";
    protected static final String CCD_ALL_CASES = "CCD:all-cases-access";
    protected static final String ORGANISATIONID = "OrganisationId";

    public CaseDataAccessTypeRolesUtils() {

    }

    private void updateCaseAccessGroupsInCaseDetails(CaseDetails caseDetails, CaseTypeDefinition caseTypeDefinition){

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

        List<AccessTypeRolesDefinition> accessTypeRolesDefinition = caseTypeDefinition.getAccessTypeRoles();

        /*
        3. For each retained record, create a case group ID value by substituting the organisation policy
           Organisation.OrganisationID value in CaseTypeOrganisationAccess.CaseGroupIDTemplate. For example :
           . CaseGroupIDTemplate is set to - CIVIL:bulk:[RESPONDENT01SOLICITOR]:$ORGID$
           . Organisation.OrganisationID value set in organisation policy - 550e8400-e29b-41d4-a716-446655440000
           . Substituting Organisation.OrganisationID value in CaseGroupIDTemplate to have a new case group ID
              - CIVIL:bulk:[RESPONDENT01SOLICITOR]:550e8400-e29b-41d4-a716-446655440000
        */

        /*
        4. Add each case group ID to the caseAccessGroups collection in the case data, setting
           caseGroupType = CCD:all-cases-access
        */


        // need to get the case access group and save it in the access type role definition
/*
        caseTypeDefinitiongetAccessTypeRolesDefinitions()
            .forEach(record -> record.getGroupRoleName())
        ;
*/
    }

    public CaseDetails removeCCDAllCasesAccessFromCaseAccessGroups(CaseDetails currentCaseDetails) {
        /*
        1. Remove all items from the case data caseAccessGroups collection where caseGroupType = CCD:all-cases-access.
        */
        CaseDetails results = currentCaseDetails;
        List<CaseAccessGroup> caseAccessGroups = getCaseAccessGroupFromCaseData(currentCaseDetails.getData());
        results.getData().entrySet().removeIf(caseDetails -> !isCCDAllCasesAccess(caseAccessGroups, currentCaseDetails));
        return (CaseDetails) results;
    }

    private List<CaseAccessGroup> getCaseAccessGroupFromCaseData(Map<String, JsonNode> caseData) {
        JsonNode caseAccessGroupJsonNode = caseData.get(CaseAccessGroups.CASE_ACCESS_GROUPS_FIELD_ID);
        if (caseAccessGroupJsonNode != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                return Collections.singletonList(objectMapper.readValue(caseAccessGroupJsonNode.toString(), CaseAccessGroup.class));
            } catch (JsonProcessingException e) {
                throw new ValidationException(FAILED_TO_READ_CASE_ACCESS_GROUPS_FROM_CASE_DATA);
            }
        }
        return null;
    }

    private boolean isCCDAllCasesAccess(List<CaseAccessGroup> caseAccessGroups, CaseDetails caseDetails) {
        boolean isCCDAllCasesAccess = false;
        for (CaseAccessGroup caseAccessGroup : caseAccessGroups) {
            if (caseAccessGroup.getCaseAccessGroupType().equals(CCD_ALL_CASES)) {
                isCCDAllCasesAccess = true;
                break;
            }
        }
        return isCCDAllCasesAccess;
    }

    private void filterAccessRoles(CaseDetails caseDetails,
                                   List<AccessTypeRolesDefinition> accessTypeRolesDefinitions) {
        /*
        2. Filter all AccessTypeRole records (joined with their owning AccessType records), keeping records where:
            . GroupRoleName is not empty.
            . CaseAssignedRoleField has a value, and the field exists in the case data.
              (Note that this may be a field of type OrganisationPolicy, or a complex type field with equivalent
              structure.)
            . For the field retrieved from the case data, Organisation.OrganisationID has a non-empty value.
        */
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
                .filter(element -> getCaseDetailsWithCaseAssignedRoleField(caseDetails, element.getCaseAssignedRoleField()).hasCaseReference())
                .collect(Collectors.toList());

     /*   Map<CaseDetails, List<CaseAssignedUserRoleWithOrganisation>> caseUserRolesWhichHaveAnOrgId =
            cauRolesByCaseDetails.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
                    // filter out no organisation_id and [CREATOR] case role
                    .filter(caseUserRole ->
                        StringUtils.isNoneBlank(caseUserRole.getOrganisationId())
                            && !caseUserRole.getCaseRole().equalsIgnoreCase(CREATOR.getRole()))
                    .collect(Collectors.toList())))
                // filter cases that have no remaining roles
                .entrySet().stream().filter(e -> !e.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                */
    }



    private CaseDetails getCaseDetailsWithCaseAssignedRoleField(CaseDetails caseDetails, String caseAssignedRoleField) {
        Map<String, JsonNode> caseAccessGroupJsonNode = caseDetails.getData();
         /*
        2. Filter all AccessTypeRole records (joined with their owning AccessType records), keeping records where:
            . GroupRoleName is not empty.
            . CaseAssignedRoleField has a value, and the field exists in the case data.
              (Note that this may be a field of type OrganisationPolicy, or a complex type field with equivalent
              structure.)
            . For the field retrieved from the case data, Organisation.OrganisationID has a non-empty value.
        */

        if (caseDetails.getData().entrySet().contains(caseAssignedRoleField)) {
            Map<String, JsonNode> withcaseAssignedRoleField = (Map<String, JsonNode>) caseAccessGroupJsonNode.entrySet().stream()
                .filter(cd -> cd.getKey().equals(caseAssignedRoleField) &&
                    StringUtils.isNoneBlank(cd.getValue().toString()))
                //cd.getKey().equals(ORGANISATIONID) &&
                //StringUtils.isNoneBlank(cd.getValue().toString())
                .collect(Collectors.toList());

            CaseDetails caseDetailsWithCaseAssignedRoleField = new CaseDetails();
            caseDetailsWithCaseAssignedRoleField.setData(withcaseAssignedRoleField);
            return caseDetailsWithCaseAssignedRoleField;
        }
        return null;
    }

}
