package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.builder;

import java.util.List;
import java.util.Set;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
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
    private final CaseDataAccessControl caseDataAccessControl;

    @Autowired
    public AccessControlGrantTypeESQueryBuilder(BasicGrantTypeESQueryBuilder basicGrantTypeQueryBuilder,
                                                SpecificGrantTypeESQueryBuilder specificGrantTypeQueryBuilder,
                                                StandardGrantTypeESQueryBuilder standardGrantTypeQueryBuilder,
                                                ChallengedGrantTypeESQueryBuilder challengedGrantTypeQueryBuilder,
                                                ExcludedGrantTypeESQueryBuilder excludedGrantTypeQueryBuilder,
                                                @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                                CaseDefinitionRepository caseDefinitionRepository,
                                                RoleAssignmentService roleAssignmentService,
                                                UserAuthorisation userAuthorisation,
                                                CaseDataAccessControl caseDataAccessControl) {
        this.basicGrantTypeQueryBuilder = basicGrantTypeQueryBuilder;
        this.specificGrantTypeQueryBuilder = specificGrantTypeQueryBuilder;
        this.standardGrantTypeQueryBuilder = standardGrantTypeQueryBuilder;
        this.challengedGrantTypeQueryBuilder = challengedGrantTypeQueryBuilder;
        this.excludedGrantTypeQueryBuilder = excludedGrantTypeQueryBuilder;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.roleAssignmentService = roleAssignmentService;
        this.userAuthorisation = userAuthorisation;
        this.caseDataAccessControl = caseDataAccessControl;
    }

    public void createQuery(String caseTypeId, BoolQueryBuilder mainQuery,
                            List<CaseStateDefinition> caseStates) {
        CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);
        List<RoleAssignment> roleAssignments = getRoleAssignments(caseTypeDefinition);
        Set<AccessProfile> accessProfiles = getAccessProfiles(roleAssignments, caseTypeDefinition);

        BoolQueryBuilder standardQuery = standardGrantTypeQueryBuilder
            .createQuery(roleAssignments, caseStates, accessProfiles);

        BoolQueryBuilder challengedQuery = challengedGrantTypeQueryBuilder
            .createQuery(roleAssignments, caseStates, accessProfiles);

        BoolQueryBuilder orgQuery = prepareQuery(standardQuery, challengedQuery);

        BoolQueryBuilder basicQuery = basicGrantTypeQueryBuilder
            .createQuery(roleAssignments, caseStates, accessProfiles);

        BoolQueryBuilder specificQuery = specificGrantTypeQueryBuilder
            .createQuery(roleAssignments, caseStates, accessProfiles);

        BoolQueryBuilder nonOrgQuery = prepareQuery(basicQuery, specificQuery);

        BoolQueryBuilder excludedQuery = excludedGrantTypeQueryBuilder
            .createQuery(roleAssignments, caseStates, accessProfiles);

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
        return roleAssignmentService.getRoleAssignments(userAuthorisation.getUserId(),
            caseTypeDefinition);
    }

    private Set<AccessProfile> getAccessProfiles(List<RoleAssignment> roleAssignments,
                                                 CaseTypeDefinition caseTypeDefinition) {
        return caseDataAccessControl.filteredAccessProfiles(roleAssignments,
            caseTypeDefinition,
            false);
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
