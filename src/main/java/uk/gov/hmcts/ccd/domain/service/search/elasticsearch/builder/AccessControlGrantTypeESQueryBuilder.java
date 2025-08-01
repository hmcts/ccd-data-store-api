package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class AccessControlGrantTypeESQueryBuilder {

    private final BasicGrantTypeESQueryBuilder basicGrantTypeQueryBuilder;
    private final SpecificGrantTypeESQueryBuilder specificGrantTypeQueryBuilder;
    private final StandardGrantTypeESQueryBuilder standardGrantTypeQueryBuilder;
    private final ChallengedGrantTypeESQueryBuilder challengedGrantTypeQueryBuilder;
    private final ExcludedGrantTypeESQueryBuilder excludedGrantTypeQueryBuilder;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseDataAccessControl caseDataAccessControl;

    @Autowired
    public AccessControlGrantTypeESQueryBuilder(BasicGrantTypeESQueryBuilder basicGrantTypeQueryBuilder,
                                                SpecificGrantTypeESQueryBuilder specificGrantTypeQueryBuilder,
                                                StandardGrantTypeESQueryBuilder standardGrantTypeQueryBuilder,
                                                ChallengedGrantTypeESQueryBuilder challengedGrantTypeQueryBuilder,
                                                ExcludedGrantTypeESQueryBuilder excludedGrantTypeQueryBuilder,
                                                @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                                CaseDefinitionRepository caseDefinitionRepository,
                                                CaseDataAccessControl caseDataAccessControl) {
        this.basicGrantTypeQueryBuilder = basicGrantTypeQueryBuilder;
        this.specificGrantTypeQueryBuilder = specificGrantTypeQueryBuilder;
        this.standardGrantTypeQueryBuilder = standardGrantTypeQueryBuilder;
        this.challengedGrantTypeQueryBuilder = challengedGrantTypeQueryBuilder;
        this.excludedGrantTypeQueryBuilder = excludedGrantTypeQueryBuilder;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseDataAccessControl = caseDataAccessControl;
    }

    public void createQuery(String caseTypeId, List<Query> mainFilterList) {
        CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);
        List<RoleAssignment> roleAssignments = getRoleAssignments(caseTypeDefinition);

        Query standardQuery = standardGrantTypeQueryBuilder.createQuery(roleAssignments, caseTypeDefinition);
        Query challengedQuery = challengedGrantTypeQueryBuilder.createQuery(roleAssignments, caseTypeDefinition);
        Query orgQuery = prepareQuery(standardQuery, challengedQuery);

        Query basicQuery = basicGrantTypeQueryBuilder.createQuery(roleAssignments, caseTypeDefinition);
        Query specificQuery = specificGrantTypeQueryBuilder.createQuery(roleAssignments, caseTypeDefinition);
        Query nonOrgQuery = prepareQuery(basicQuery, specificQuery);

        Query excludedQuery = excludedGrantTypeQueryBuilder.createQuery(roleAssignments, caseTypeDefinition);

        boolean hasNonOrg = hasClauses(nonOrgQuery);
        boolean hasOrg = hasClauses(orgQuery);
        boolean hasExcluded = hasClauses(excludedQuery);

        if (!hasNonOrg && !hasOrg && hasExcluded) {
            mainFilterList.add(Query.of(q -> q.bool(b -> b.mustNot(List.of(excludedQuery)))));
        }

        if (!hasNonOrg && hasOrg && !hasExcluded) {
            mainFilterList.add(orgQuery);
        }

        if (hasNonOrg && !hasOrg && !hasExcluded) {
            mainFilterList.add(nonOrgQuery);
        }

        if (!hasNonOrg && hasOrg && hasExcluded) {
            Query orgWithExclusion = Query.of(q -> q.bool(b -> b
                .must(List.of(orgQuery))
                .mustNot(List.of(excludedQuery))
            ));
            mainFilterList.add(orgWithExclusion);
        }

        if (hasNonOrg && !hasOrg && hasExcluded) {
            Query nonOrgWithExclusion = Query.of(q -> q.bool(b -> b
                .must(List.of(nonOrgQuery))
                .mustNot(List.of(excludedQuery))
            ));
            mainFilterList.add(nonOrgWithExclusion);
        }

        if (hasNonOrg && hasOrg && !hasExcluded) {
            Query combined = Query.of(q -> q.bool(b -> b
                .must(List.of(nonOrgQuery))
                .should(List.of(orgQuery))
            ));
            mainFilterList.add(combined);
        }

        if (hasNonOrg && hasOrg && hasExcluded) {
            Query orgWithExclusion = Query.of(q -> q.bool(b -> b
                .must(List.of(orgQuery))
                .mustNot(List.of(excludedQuery))
            ));

            Query combined = Query.of(q -> q.bool(b -> b
                .must(List.of(nonOrgQuery))
                .should(List.of(orgWithExclusion))
            ));

            mainFilterList.add(combined);
        }
    }

    private List<RoleAssignment> getRoleAssignments(CaseTypeDefinition caseTypeDefinition) {
        return caseDataAccessControl.generateRoleAssignments(caseTypeDefinition);
    }

    private Query prepareQuery(Query queryOne, Query queryTwo) {
        List<Query> shoulds = new ArrayList<>();

        if (hasClauses(queryOne)) {
            shoulds.add(queryOne);
        }
        if (hasClauses(queryTwo)) {
            shoulds.add(queryTwo);
        }

        if (shoulds.isEmpty()) {
            return emptyBoolQuery();
        }

        return Query.of(q -> q.bool(b -> b.should(shoulds)));
    }

    private boolean hasClauses(Query query) {
        if (query == null) {
            return false;
        }
        return Optional.ofNullable(query.bool())
            .map(BoolQuery::should)
            .map(list -> !list.isEmpty())
            .orElse(false)
            ||
            Optional.ofNullable(query.bool())
                .map(BoolQuery::must)
                .map(list -> !list.isEmpty())
                .orElse(false)
            ||
            Optional.ofNullable(query.bool())
                .map(BoolQuery::filter)
                .map(list -> !list.isEmpty())
                .orElse(false)
            ||
            Optional.ofNullable(query.bool())
                .map(BoolQuery::mustNot)
                .map(list -> !list.isEmpty())
                .orElse(false);
    }

    private Query emptyBoolQuery() {
        return Query.of(q -> q.bool(b -> b));
    }
}
