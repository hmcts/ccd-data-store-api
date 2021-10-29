package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

public interface GrantTypeQueryBuilder {

    String QUERY_WRAPPER = "( %s )";

    String QUERY = "%s in (:%s)";

    String EMPTY = "";

    String SECURITY_CLASSIFICATION = "security_classification";

    String STATES = "states";

    String JURISDICTION = "jurisdiction";

    String REFERENCE = "reference";

    String CASE_TYPE_ID = "case_type_id";

    String LOCATION = "data" + " #>> '{caseManagementLocation,baseLocation}'";

    String REGION = "data" + " #>> '{caseManagementLocation,region}'";

    String AND = " AND ";

    String OR = " OR ";

    String AND_NOT = " AND NOT ";

    String createQuery(List<RoleAssignment> roleAssignments,
                       Map<String, Object> params,
                       List<CaseStateDefinition> caseStates,
                       Set<AccessProfile> accessProfiles);

    default String createClassification(Map<String, Object> params, String paramName,
                                        Supplier<Stream<RoleAssignment>> streamSupplier,
                                        AccessControlService accessControlService,
                                        List<CaseStateDefinition> caseStates,
                                        Set<AccessProfile> accessProfiles) {
        Set<String> classifications = streamSupplier
            .get()
            .map(roleAssignment -> roleAssignment.getClassification())
            .filter(classification -> StringUtils.isNotBlank(classification))
            .collect(Collectors.toSet());

        Set<String> classificationParams = getClassificationParams(classifications);

        String tmpQuery = EMPTY;

        if (classificationParams.size() > 0) {
            String classificationsParam = "classifications_" + paramName;
            params.put(classificationsParam, classificationParams);
            tmpQuery = String.format(QUERY, SECURITY_CLASSIFICATION, classificationsParam);
        }

        List<CaseStateDefinition> raCaseStates = accessControlService
            .filterCaseStatesByAccess(caseStates, accessProfiles, CAN_READ);

        if (!raCaseStates.isEmpty()) {
            String statesParam = "states_" + paramName;
            params.put(statesParam, raCaseStates);
            return tmpQuery + getOperator(tmpQuery, AND) + String.format(QUERY, STATES, statesParam);
        }

        return tmpQuery;
    }

    private Set<String> getClassificationParams(Set<String> classifications) {
        if (classifications.contains(SecurityClassification.RESTRICTED.name())) {
            return Sets.newHashSet(SecurityClassification.PUBLIC.name(),
                SecurityClassification.PRIVATE.name(),
                SecurityClassification.RESTRICTED.name());
        } else if (classifications.contains(SecurityClassification.PRIVATE.name())) {
            return Sets.newHashSet(SecurityClassification.PUBLIC.name(),
                SecurityClassification.PRIVATE.name());
        } else if (classifications.contains(SecurityClassification.PUBLIC.name())) {
            return Sets.newHashSet(SecurityClassification.PUBLIC.name());
        }
        return classifications;
    }

    default String getOperator(String query, String operator) {
        if (StringUtils.isNotBlank(query)) {
            return operator;
        }
        return EMPTY;
    }
}
