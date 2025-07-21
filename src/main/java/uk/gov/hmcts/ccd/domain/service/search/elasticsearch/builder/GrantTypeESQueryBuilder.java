package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.casedetails.search.builder.GrantTypeQueryBuilder;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.SearchRoleAssignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    public Query createQuery(List<RoleAssignment> roleAssignments, CaseTypeDefinition caseType) {
        List<CaseStateDefinition> caseStates = getStatesForCaseType(caseType);
        List<Query> shouldQueries = new ArrayList<>();

        getGroupedSearchRoleAssignments(roleAssignments)
            .forEach((hash, groupedAssignments) -> {
                SearchRoleAssignment representative = groupedAssignments.get(0);
                Set<String> readableStates = getReadableCaseStates(representative, caseStates, caseType);
                if (readableStates.isEmpty()) {
                    return;
                }

                List<Query> innerMustQueries = new ArrayList<>();

                if (getApplicationParams().getCaseGroupAccessFilteringEnabled()) {
                    addTermQueryForOptionalAttribute(representative.getCaseAccessGroupId(),
                        CASE_ACCESS_GROUP_ID_FIELD_COL, innerMustQueries);
                }
                addTermQueryForOptionalAttribute(representative.getJurisdiction(), JURISDICTION_FIELD_COL,
                    innerMustQueries);
                addTermQueryForOptionalAttribute(representative.getRegion(), REGION, innerMustQueries);
                addTermQueryForOptionalAttribute(representative.getLocation(), LOCATION, innerMustQueries);

                addTermsQueryForReference(groupedAssignments, innerMustQueries);
                addTermsQueryForState(readableStates, caseStates, innerMustQueries);
                addTermsQueryForClassification(representative, innerMustQueries);
                addPrefixQueryForCaseAccessCategory(caseType, representative, innerMustQueries);

                if (!innerMustQueries.isEmpty()) {
                    shouldQueries.add(Query.of(q -> q.bool(b -> b.must(innerMustQueries))));
                }
            });

        if (shouldQueries.isEmpty()) {
            return Query.of(q -> q.bool(b -> b)); // empty bool query
        }

        return Query.of(q -> q.bool(b -> b.should(shouldQueries).minimumShouldMatch("1")));
    }

    private void addTermsQueryForState(Set<String> readableStates,
                                       List<CaseStateDefinition> allStates,
                                       List<Query> mustQueries) {
        if (readableStates.size() != allStates.size()) {
            mustQueries.add(Query.of(q -> q.terms(t -> t
                .field(STATE_FIELD_COL + KEYWORD)
                .terms(TermsQueryField.of(f -> f
                    .value(readableStates.stream()
                        .map(FieldValue::of)
                        .collect(Collectors.toList()))
                ))
            )));
        }
    }

    private void addTermsQueryForReference(List<SearchRoleAssignment> assignments, List<Query> mustQueries) {
        if (allRoleAssignmentsHaveCaseReference(assignments)) {
            List<String> references = assignments.stream()
                .map(SearchRoleAssignment::getCaseReference)
                .collect(Collectors.toList());

            mustQueries.add(Query.of(q -> q.terms(t -> t
                .field(REFERENCE_FIELD_COL + KEYWORD)
                .terms(TermsQueryField.of(f -> f
                    .value(references.stream()
                        .map(FieldValue::of)
                        .collect(Collectors.toList()))
                ))
            )));
        }
    }

    private void addTermQueryForOptionalAttribute(String attribute, String field, List<Query> mustQueries) {
        if (StringUtils.isNotBlank(attribute)) {
            mustQueries.add(Query.of(q -> q.term(t -> t
                .field(field + KEYWORD)
                .value(attribute)
            )));
        }
    }

    private void addTermsQueryForClassification(SearchRoleAssignment role, List<Query> mustQueries) {
        List<String> classifications = getClassifications(role);
        if (!classifications.isEmpty()) {
            mustQueries.add(Query.of(q -> q.terms(t -> t
                .field(SECURITY_CLASSIFICATION_FIELD_COL)
                .terms(TermsQueryField.of(f -> f
                    .value(classifications.stream()
                        .map(FieldValue::of)
                        .collect(Collectors.toList()))
                ))
            )));
        }
    }

    private void addPrefixQueryForCaseAccessCategory(CaseTypeDefinition caseType,
                                                     SearchRoleAssignment role,
                                                     List<Query> mustQueries) {
        List<String> categories = getCaseAccessCategories(role.getRoleAssignment(), caseType);
        if (categories.isEmpty()) {
            return;
        }

        List<Query> shouldPrefixes = categories.stream()
            .map(cac -> Query.of(q -> q.matchPhrasePrefix(mpp -> mpp
                .field(CASE_ACCESS_CATEGORY)
                .query(cac)
            )))
            .collect(Collectors.toList());

        mustQueries.add(Query.of(q -> q.bool(b -> b.should(shouldPrefixes))));
    }
}
