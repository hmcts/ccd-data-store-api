package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;

public interface GrantTypeQueryBuilder {

    String QUERY_WRAPPER = "( %s )";

    String QUERY = "%s in (:%s)";

    String EMPTY = "";

    String SECURITY_CLASSIFICATION = "security_classification";

    String JURISDICTION = "jurisdiction";

    String REFERENCE = "reference";

    String CASE_TYPE_ID = "case_type_id";

    String LOCATION = "data" + " #>> '{caseManagementLocation,baseLocation}'";

    String REGION = "data" + " #>> '{caseManagementLocation,region}'";

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

        Set<String> classificationParams = getClassificationParams(classifications);

        if (classificationParams.size() > 0) {
            params.put(paramName, classificationParams);
            return String.format(QUERY, SECURITY_CLASSIFICATION, paramName);
        }

        return EMPTY;
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
