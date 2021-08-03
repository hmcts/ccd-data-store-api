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
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.RoleAssignmentService;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

@Component
public class AccessControlGrantTypeESQueryBuilder {

    private final BasicGrantTypeESQueryBuilder basicGrantTypeQueryBuilder;
    private final SpecificGrantTypeESQueryBuilder specificGrantTypeQueryBuilder;
    private final StandardGrantTypeESQueryBuilder standardGrantTypeQueryBuilder;
    private final ChallengedGrantTypeESQueryBuilder challengedGrantTypeQueryBuilder;
    private final ExcludedGrantTypeESQueryBuilder excludedGrantTypeQueryBuilder;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final RoleAssignmentService roleAssignmentService;
    private final UserAuthorisation userAuthorisation;

    @Autowired
    public AccessControlGrantTypeESQueryBuilder(BasicGrantTypeESQueryBuilder basicGrantTypeQueryBuilder,
                                                SpecificGrantTypeESQueryBuilder specificGrantTypeQueryBuilder,
                                                StandardGrantTypeESQueryBuilder standardGrantTypeQueryBuilder,
                                                ChallengedGrantTypeESQueryBuilder challengedGrantTypeQueryBuilder,
                                                ExcludedGrantTypeESQueryBuilder excludedGrantTypeQueryBuilder,
                                                @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                                CaseDefinitionRepository caseDefinitionRepository,
                                                RoleAssignmentService roleAssignmentService,
                                                UserAuthorisation userAuthorisation) {
        this.basicGrantTypeQueryBuilder = basicGrantTypeQueryBuilder;
        this.specificGrantTypeQueryBuilder = specificGrantTypeQueryBuilder;
        this.standardGrantTypeQueryBuilder = standardGrantTypeQueryBuilder;
        this.challengedGrantTypeQueryBuilder = challengedGrantTypeQueryBuilder;
        this.excludedGrantTypeQueryBuilder = excludedGrantTypeQueryBuilder;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.roleAssignmentService = roleAssignmentService;
        this.userAuthorisation = userAuthorisation;
    }

    public BoolQueryBuilder createQuery(String caseTypeId) {
        List<RoleAssignment> roleAssignments = getRoleAssignments(caseTypeId);

        BoolQueryBuilder standardQuery = standardGrantTypeQueryBuilder.createQuery(roleAssignments);
        BoolQueryBuilder challengedQuery = challengedGrantTypeQueryBuilder.createQuery(roleAssignments);
        BoolQueryBuilder orgQuery = QueryBuilders.boolQuery();

        if (standardQuery.hasClauses()) {
            orgQuery.should(standardQuery);
        }
        if (challengedQuery.hasClauses()) {
            orgQuery.should(challengedQuery);
        }

        BoolQueryBuilder orgQueryWithExcluded = QueryBuilders.boolQuery();

        BoolQueryBuilder excludedQuery = excludedGrantTypeQueryBuilder.createQuery(roleAssignments);
        if (excludedQuery.hasClauses()) {
            orgQueryWithExcluded.mustNot(excludedQuery);
        }

        if (orgQuery.hasClauses()) {
            orgQueryWithExcluded.should(orgQuery);
        }

        BoolQueryBuilder basicQuery = basicGrantTypeQueryBuilder.createQuery(roleAssignments);
        BoolQueryBuilder specificQuery = specificGrantTypeQueryBuilder.createQuery(roleAssignments);

        BoolQueryBuilder nonOrgQuery = QueryBuilders.boolQuery();
        if (basicQuery.hasClauses()) {
            nonOrgQuery.should(basicQuery);
        }

        if (specificQuery.hasClauses()) {
            nonOrgQuery.should(specificQuery);
        }

        BoolQueryBuilder grantTypeQuery = QueryBuilders.boolQuery();
        if (nonOrgQuery.hasClauses()) {
            grantTypeQuery.should(nonOrgQuery);
        }

        if (orgQueryWithExcluded.hasClauses()) {
            grantTypeQuery.should(orgQueryWithExcluded);
        }

        return grantTypeQuery;
    }

    private List<RoleAssignment> getRoleAssignments(String caseTypeId) {
        CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);
        return roleAssignmentService.getRoleAssignments(userAuthorisation.getUserId(), caseTypeDefinition);
    }
}
