package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.casedetails.search.builder.GrantTypeQueryBuilder;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.SearchRoleAssignment;

import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.CASE_ACCESS_CATEGORY;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.JURISDICTION_FIELD_COL;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.CASE_ACCESS_GROUP_ID_FIELD_COL;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.LOCATION;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.REFERENCE_FIELD_COL;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.REGION;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.SECURITY_CLASSIFICATION_FIELD_COL;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.STATE_FIELD_COL;

@Slf4j
public abstract class GrantTypeESQueryBuilder extends GrantTypeQueryBuilder {

    protected static final String KEYWORD = ".keyword";

    protected GrantTypeESQueryBuilder(AccessControlService accessControlService,
                                      CaseDataAccessControl caseDataAccessControl,
                                      ApplicationParams applicationParams) {
        super(accessControlService, caseDataAccessControl, applicationParams);
    }

    public BoolQueryBuilder createQuery(List<RoleAssignment> roleAssignments,
                                        CaseTypeDefinition caseType) {
        List<CaseStateDefinition> caseStates = getStatesForCaseType(caseType);
        BoolQueryBuilder query = QueryBuilders.boolQuery();

        getGroupedSearchRoleAssignments(roleAssignments)
            .forEach((hash, groupedSearchRoleAssignments) -> {
                BoolQueryBuilder innerQuery = QueryBuilders.boolQuery();
                SearchRoleAssignment representative = groupedSearchRoleAssignments.get(0);
                Set<String> readableCaseStates = getReadableCaseStates(representative, caseStates, caseType);
                if (readableCaseStates.isEmpty()) {
                    return;
                }

                if (getApplicationParams().getCaseGroupAccessFilteringEnabled()) {
                    addTermQueryForOptionalAttribute(representative.getCaseAccessGroupId(), innerQuery,
                        CASE_ACCESS_GROUP_ID_FIELD_COL);
                }
                addTermQueryForOptionalAttribute(representative.getJurisdiction(), innerQuery, JURISDICTION_FIELD_COL);
                addTermQueryForOptionalAttribute(representative.getRegion(), innerQuery, REGION);
                addTermQueryForOptionalAttribute(representative.getLocation(), innerQuery, LOCATION);
                addTermsQueryForReference(groupedSearchRoleAssignments, innerQuery);
                addTermsQueryForState(readableCaseStates, caseStates, innerQuery);
                addTermsQueryForClassification(representative, innerQuery);
                addPrefixQueryForCaseAccessCategory(caseType, representative, innerQuery);

                query.should(innerQuery);
            });

        return query;
    }

    private void addTermsQueryForState(Set<String> readableCaseStates,
                                       List<CaseStateDefinition> allCaseStates,
                                       BoolQueryBuilder parentQuery) {
        if (readableCaseStates.size() != allCaseStates.size()) {
            parentQuery.must(QueryBuilders.termsQuery(STATE_FIELD_COL + KEYWORD, readableCaseStates));
        }
    }

    private void addTermsQueryForReference(List<SearchRoleAssignment> searchRoleAssignments,
                                           BoolQueryBuilder parentQuery) {
        if (allRoleAssignmentsHaveCaseReference(searchRoleAssignments)) {
            parentQuery.must(QueryBuilders.termsQuery(REFERENCE_FIELD_COL + KEYWORD,
                searchRoleAssignments.stream()
                    .map(SearchRoleAssignment::getCaseReference)
                    .collect(Collectors.toList())));
        }
    }

    private void addTermQueryForOptionalAttribute(String attribute,
                                                  BoolQueryBuilder parentQuery,
                                                  String matchName) {
        if (StringUtils.isNotBlank(attribute)) {
            parentQuery.must(QueryBuilders.termQuery(matchName + KEYWORD, attribute));
        }
    }

    private void addTermsQueryForClassification(SearchRoleAssignment searchRoleAssignment,
                                                BoolQueryBuilder parentQuery) {
        List<String> classifications = getClassifications(searchRoleAssignment);
        if (!classifications.isEmpty()) {
            parentQuery.must(QueryBuilders.termsQuery(SECURITY_CLASSIFICATION_FIELD_COL, classifications));
        }
    }

    private void addPrefixQueryForCaseAccessCategory(CaseTypeDefinition caseType,
                                                     SearchRoleAssignment representative,
                                                     BoolQueryBuilder parentQuery) {

        List<String> caseAccessCategories = getCaseAccessCategories(representative.getRoleAssignment(), caseType);
        if (caseAccessCategories.isEmpty()) {
            return;
        }

        BoolQueryBuilder caseAccessQuery = QueryBuilders.boolQuery();

        caseAccessCategories
            .forEach(cac -> caseAccessQuery.should(QueryBuilders.matchPhrasePrefixQuery(CASE_ACCESS_CATEGORY, cac)));
        parentQuery.must(caseAccessQuery);
    }
}
