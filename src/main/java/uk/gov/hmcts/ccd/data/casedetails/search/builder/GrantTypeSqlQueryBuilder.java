package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.SearchRoleAssignment;

public abstract class GrantTypeSqlQueryBuilder extends GrantTypeQueryBuilder {

    public static final String OR = " OR ";

    public static final String AND_NOT = " AND NOT ";

    public static final String QUERY_WRAPPER = "( %s )";

    public static final String QUERY = "%s in (:%s)";

    public static final String EMPTY = "";

    public static final String SECURITY_CLASSIFICATION = "security_classification";

    public static final String STATES = "state";

    public static final String JURISDICTION = "jurisdiction";

    public static final String REFERENCE = "reference";

    public static final String LOCATION = "data" + " #>> '{caseManagementLocation,baseLocation}'";

    public static final String REGION = "data" + " #>> '{caseManagementLocation,region}'";

    public static final String AND = " AND ";

    public static final String CASE_STATES_PARAM = "states_%s_%s";

    public static final String CLASSIFICATIONS_PARAM = "classifications_%s_%s";

    public static final String REFERENCES_PARAM = "references_%s_%s";

    public static final String CASE_ACCESS_CATEGORY = "data" + " #>> '{CaseAccessCategory}'";

    protected GrantTypeSqlQueryBuilder(AccessControlService accessControlService,
                                       CaseDataAccessControl caseDataAccessControl,
                                       ApplicationParams applicationParams) {
        super(accessControlService, caseDataAccessControl, applicationParams);
    }

    public String createQuery(List<RoleAssignment> roleAssignments,
                              Map<String, Object> params,
                              CaseTypeDefinition caseType) {
        String paramName = getGrantType().name().toLowerCase();
        List<CaseStateDefinition> caseStates = getStatesForCaseType(caseType);
        AtomicInteger index = new AtomicInteger();

        return getGroupedSearchRoleAssignments(roleAssignments)
            .values().stream()
            .map(groupedSearchRoleAssignments -> {
                final int count = index.incrementAndGet();
                String innerQuery = EMPTY;
                SearchRoleAssignment representative = groupedSearchRoleAssignments.get(0);
                Set<String> readableCaseStates = getReadableCaseStates(representative, caseStates, caseType);
                if (readableCaseStates.isEmpty()) {
                    return innerQuery;
                }

                innerQuery = addOptionalInQueryForCaseGroupId(representative.getCaseAccessGroupId(),
                    innerQuery);
                innerQuery = addEqualsQueryForOptionalAttribute(representative.getJurisdiction(),
                    innerQuery, JURISDICTION);
                innerQuery = addEqualsQueryForOptionalAttribute(representative.getRegion(),
                    innerQuery, REGION);
                innerQuery = addEqualsQueryForOptionalAttribute(representative.getLocation(),
                    innerQuery, LOCATION);
                innerQuery = addInQueryForReference(params, paramName, innerQuery,
                    groupedSearchRoleAssignments, count);
                innerQuery = addInQueryForState(params, paramName, readableCaseStates, caseStates, innerQuery, count);
                innerQuery = addInQueryForClassification(params, paramName, innerQuery, representative, count);
                innerQuery = addInQueryForCaseAccessCategory(caseType, representative, innerQuery);

                return StringUtils.isNotBlank(innerQuery) ? String.format(QUERY_WRAPPER, innerQuery) : innerQuery;
            }).filter(strQuery -> !StringUtils.isEmpty(strQuery)).collect(Collectors.joining(OR));
    }

    private String addInQueryForState(Map<String, Object> params,
                                      String paramName,
                                      Set<String> readableCaseStates,
                                      List<CaseStateDefinition> allCaseStates,
                                      String parentQuery,
                                      int count) {
        if (readableCaseStates.size() != allCaseStates.size()) {
            String statesParam = String.format(CASE_STATES_PARAM, count, paramName);
            params.put(statesParam, readableCaseStates);
            parentQuery = parentQuery + getOperator(parentQuery, AND)
                + String.format(QUERY, STATES, statesParam);
        }
        return parentQuery;
    }

    private String addOptionalInQueryForCaseGroupId(String caseAccessGroupId, String parentQuery) {
        if (!getApplicationParams().getCaseGroupAccessFilteringEnabled()) {
            return parentQuery;
        }
        if (StringUtils.isBlank(caseAccessGroupId)) {
            return parentQuery;
        }
        return parentQuery + getOperator(parentQuery, AND)
            + "data->'CaseAccessGroups' @> '[{\"value\":{\"caseAccessGroupId\": \"" + caseAccessGroupId + "\"}}]'";
    }

    private String addInQueryForReference(Map<String, Object> params,
                                          String paramName,
                                          String parentQuery,
                                          List<SearchRoleAssignment> searchRoleAssignments,
                                          int index) {
        if (allRoleAssignmentsHaveCaseReference(searchRoleAssignments)) {
            String referencesParam = String.format(REFERENCES_PARAM, index, paramName);
            params.put(referencesParam, searchRoleAssignments.stream()
                .map(SearchRoleAssignment::getCaseReference)
                .collect(Collectors.toList()));
            parentQuery = parentQuery + getOperator(parentQuery, AND)
                + String.format(QUERY, REFERENCE, referencesParam);
        }
        return parentQuery;
    }

    private String addInQueryForClassification(Map<String, Object> params,
                                               String paramName,
                                               String parentQuery,
                                               SearchRoleAssignment searchRoleAssignment,
                                               int index) {
        List<String> classifications = getClassifications(searchRoleAssignment);
        if (!classifications.isEmpty()) {
            String classificationsParam = String.format(CLASSIFICATIONS_PARAM, index, paramName);
            params.put(classificationsParam, classifications);
            parentQuery = parentQuery + getOperator(parentQuery, AND)
                + String.format(QUERY, SECURITY_CLASSIFICATION, classificationsParam);
        }
        return parentQuery;
    }

    private String addEqualsQueryForOptionalAttribute(String attribute,
                                                      String parentQuery,
                                                      String matchName) {
        if (StringUtils.isNotBlank(attribute)) {
            parentQuery = parentQuery + getOperator(parentQuery, AND)
                + String.format("%s='%s'", matchName, attribute);
        }
        return parentQuery;
    }

    public String getOperator(String query, String operator) {
        if (StringUtils.isNotBlank(query)) {
            return operator;
        }
        return EMPTY;
    }

    private String addInQueryForCaseAccessCategory(CaseTypeDefinition caseType,
                                                   SearchRoleAssignment representative,
                                                   String parentQuery) {
        String caseAccessCategoriesQuery = getCaseAccessCategoriesQuery(representative.getRoleAssignment(), caseType);
        if (StringUtils.isNotBlank(caseAccessCategoriesQuery)) {
            parentQuery = parentQuery + getOperator(parentQuery, AND)
                + String.format(QUERY_WRAPPER, caseAccessCategoriesQuery);
        }
        return parentQuery;
    }

    private String getCaseAccessCategoriesQuery(RoleAssignment roleAssignment, CaseTypeDefinition caseType) {
        List<String> caseAccessCategories = getCaseAccessCategories(roleAssignment, caseType);

        return caseAccessCategories.stream()
            .map(cac -> CASE_ACCESS_CATEGORY + " LIKE '" + cac + "%'")
            .collect(Collectors.joining(" OR "));
    }
}
