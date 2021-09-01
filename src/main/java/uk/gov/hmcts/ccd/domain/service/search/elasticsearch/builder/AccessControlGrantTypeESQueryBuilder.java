package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import java.util.List;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
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

    public void createQuery(String caseTypeId, BoolQueryBuilder mainQuery) {
        List<RoleAssignment> roleAssignments = getRoleAssignments(caseTypeId);
        List<TermsQueryBuilder> standardQuery = standardGrantTypeQueryBuilder.createQuery(roleAssignments);
        List<TermsQueryBuilder> challengedQuery = challengedGrantTypeQueryBuilder.createQuery(roleAssignments);
        BoolQueryBuilder orgQuery = QueryBuilders.boolQuery();

        standardQuery.stream()
            .forEach(query -> orgQuery.should(query));
        challengedQuery.stream()
            .forEach(query -> orgQuery.should(query));

        List<TermsQueryBuilder> excludedTerms = excludedGrantTypeQueryBuilder.createQuery(roleAssignments);
        BoolQueryBuilder excludedQuery = QueryBuilders.boolQuery();
        excludedTerms.stream()
            .forEach(query -> excludedQuery.must(query));

        BoolQueryBuilder nonOrgQuery = QueryBuilders.boolQuery();
        List<TermsQueryBuilder> basicQuery = basicGrantTypeQueryBuilder.createQuery(roleAssignments);
        basicQuery.stream()
            .forEach(query -> nonOrgQuery.should(query));

        List<TermsQueryBuilder> specificQuery = specificGrantTypeQueryBuilder.createQuery(roleAssignments);
        specificQuery.stream()
            .forEach(query -> nonOrgQuery.should(query));

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

    private List<RoleAssignment> getRoleAssignments(String caseTypeId) {
        CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);
        return roleAssignmentService.getRoleAssignments(userAuthorisation.getUserId(), caseTypeDefinition);
    }
}
