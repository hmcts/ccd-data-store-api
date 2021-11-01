package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import java.util.List;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;

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

    public void createQuery(String caseTypeId, BoolQueryBuilder mainQuery) {
        CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);
        List<RoleAssignment> roleAssignments = getRoleAssignments(caseTypeDefinition);

        BoolQueryBuilder standardQuery = standardGrantTypeQueryBuilder
            .createQuery(roleAssignments, caseTypeDefinition);

        BoolQueryBuilder challengedQuery = challengedGrantTypeQueryBuilder
            .createQuery(roleAssignments, caseTypeDefinition);

        BoolQueryBuilder orgQuery = prepareQuery(standardQuery, challengedQuery);

        BoolQueryBuilder basicQuery = basicGrantTypeQueryBuilder
            .createQuery(roleAssignments, caseTypeDefinition);

        BoolQueryBuilder specificQuery = specificGrantTypeQueryBuilder
            .createQuery(roleAssignments, caseTypeDefinition);

        BoolQueryBuilder nonOrgQuery = prepareQuery(basicQuery, specificQuery);

        BoolQueryBuilder excludedQuery = excludedGrantTypeQueryBuilder
            .createQuery(roleAssignments, caseTypeDefinition);

        if (!nonOrgQuery.hasClauses()
            && !orgQuery.hasClauses()
            && excludedQuery.hasClauses()) {
            mainQuery.mustNot(excludedQuery);
        }

        if (!nonOrgQuery.hasClauses()
            && orgQuery.hasClauses()
            && !excludedQuery.hasClauses()) {
            mainQuery.must(orgQuery);
        }

        if (nonOrgQuery.hasClauses()
            && !orgQuery.hasClauses()
            && !excludedQuery.hasClauses()) {
            mainQuery.must(nonOrgQuery);
        }

        if (!nonOrgQuery.hasClauses()
            && orgQuery.hasClauses()
            && excludedQuery.hasClauses()) {
            orgQuery.mustNot(excludedQuery);
            mainQuery.must(orgQuery);
        }

        if (nonOrgQuery.hasClauses()
            && !orgQuery.hasClauses()
            && excludedQuery.hasClauses()) {
            nonOrgQuery.mustNot(excludedQuery);
            mainQuery.must(nonOrgQuery);
        }

        if (nonOrgQuery.hasClauses()
            && orgQuery.hasClauses()
            && !excludedQuery.hasClauses()) {
            nonOrgQuery.should(orgQuery);
            mainQuery.must(nonOrgQuery);
        }

        if (nonOrgQuery.hasClauses()
            && orgQuery.hasClauses()
            && excludedQuery.hasClauses()) {
            orgQuery.mustNot(excludedQuery);
            nonOrgQuery.should(orgQuery);
            mainQuery.must(nonOrgQuery);
        }
    }

    private List<RoleAssignment> getRoleAssignments(CaseTypeDefinition caseTypeDefinition) {
        return caseDataAccessControl.generateRoleAssignments(caseTypeDefinition);
    }

    private BoolQueryBuilder prepareQuery(BoolQueryBuilder queryOne, BoolQueryBuilder queryTwo) {
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        if (queryOne.hasClauses() && queryTwo.hasClauses()) {
            queryBuilder.should(queryOne);
            queryBuilder.should(queryTwo);
        } else if (queryOne.hasClauses()) {
            queryBuilder.should(queryOne);
        } else if (queryTwo.hasClauses()) {
            queryBuilder.should(queryTwo);
        }
        return queryBuilder;
    }
}
