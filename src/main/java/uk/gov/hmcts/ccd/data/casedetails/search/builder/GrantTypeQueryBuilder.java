package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;

public interface GrantTypeQueryBuilder {

    String QUERY_WRAPPER = "( %s )";

    String QUERY = "%s in (:%s)";

    String EMPTY = "";

    String SECURITY_CLASSIFICATION = "security_classification";

    String JURISDICTION = "jurisdiction";

    String REFERENCE = "reference";

    String CASE_TYPE_ID = "case_type_id";

    String LOCATION = "data" + " #>> '{caseManagementLocation,BaseLocationId}'";

    String REGION = "data" + " #>> '{caseManagementLocation,RegionId}'";

    String AND = " AND ";

    String OR = " OR ";

    String AND_NOT = " AND NOT ";

    String createQuery(List<RoleAssignment> roleAssignments, Map<String, Object> params);

    default String createClassification(Map<String, Object> params, String paramName,
                                        Stream<RoleAssignment> roleAssignmentStream) {
        Set<String> classifications = roleAssignmentStream
            .map(roleAssignment -> roleAssignment.getClassification())
            .filter(classification -> StringUtils.isNotBlank(classification))
            .collect(Collectors.toSet());

        if (classifications.size() > 0) {
            params.put(paramName, classifications);
            return String.format(QUERY, SECURITY_CLASSIFICATION, paramName);
        }

        return EMPTY;
    }

    default String getOperator(String query, String operator) {
        if (StringUtils.isNotBlank(query)) {
            return operator;
        }
        return EMPTY;
    }
}
