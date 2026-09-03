package uk.gov.hmcts.ccd.data.casedetails.search;

import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;
import uk.gov.hmcts.ccd.data.casedetails.search.builder.AccessControlGrantTypeQueryBuilder;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.security.AuthorisedCaseDefinitionDataService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

import static uk.gov.hmcts.ccd.data.casedetails.search.builder.GrantTypeSqlQueryBuilder.ACCESS_CONTROL_PARAM_PREFIX;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

@Named
@Singleton
@Slf4j
public class SearchQueryFactoryOperation {

    private static final String AND = " AND ";
    private static final String OPERATION_EQ = " = ";
    private static final String OPERATION_LIKE = " LIKE ";

    // Access control parameters live in a namespace user supplied search fields cannot reach
    // see GrantTypeSqlQueryBuilder#ACCESS_CONTROL_PARAM_PREFIX.
    static final String USER_ID_PARAM = ACCESS_CONTROL_PARAM_PREFIX + "user_id";
    static final String CASE_STATES_PARAM = ACCESS_CONTROL_PARAM_PREFIX + "states";

    @PersistenceContext
    private final EntityManager entityManager;

    private static final String MAIN_QUERY = "SELECT * FROM case_data WHERE %s ORDER BY %s";
    private static final String MAIN_COUNT_QUERY = "SELECT count(*) FROM case_data WHERE %s";

    private final CriterionFactory criterionFactory;
    private final ApplicationParams applicationParam;
    private final UserAuthorisation userAuthorisation;
    private final SortOrderQueryBuilder sortOrderQueryBuilder;
    private final AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService;
    private final AccessControlGrantTypeQueryBuilder accessControlGrantTypeQueryBuilder;
    private final CaseDataAccessControl caseDataAccessControl;
    private final CaseTypeService caseTypeService;

    public SearchQueryFactoryOperation(CriterionFactory criterionFactory,
                                       EntityManager entityManager,
                                       ApplicationParams applicationParam,
                                       UserAuthorisation userAuthorisation,
                                       SortOrderQueryBuilder sortOrderQueryBuilder,
                                       AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService,
                                       AccessControlGrantTypeQueryBuilder accessControlGrantTypeQueryBuilder,
                                       CaseDataAccessControl caseDataAccessControl,
                                       CaseTypeService caseTypeService) {
        this.criterionFactory = criterionFactory;
        this.entityManager = entityManager;
        this.applicationParam = applicationParam;
        this.userAuthorisation = userAuthorisation;
        this.sortOrderQueryBuilder = sortOrderQueryBuilder;
        this.authorisedCaseDefinitionDataService = authorisedCaseDefinitionDataService;
        this.accessControlGrantTypeQueryBuilder = accessControlGrantTypeQueryBuilder;
        this.caseDataAccessControl = caseDataAccessControl;
        this.caseTypeService = caseTypeService;
    }

    public Optional<Query> build(MetaData metadata, Map<String, String> params, boolean isCountQuery) {
        final List<Criterion> criteria = criterionFactory.build(metadata, params);

        Map<String, Object> parametersToBind = Maps.newHashMap();
        String whereClausePart = secure(toClauses(criteria), metadata, parametersToBind);
        if (whereClausePart.isEmpty()) {
            return Optional.empty();
        }
        rejectAccessControlParameterCollisions(criteria, parametersToBind);

        String sortClause = sortOrderQueryBuilder.buildSortOrderClause(metadata);
        String queryToFormat = isCountQuery ? MAIN_COUNT_QUERY : MAIN_QUERY;
        String queryString = String.format(queryToFormat, whereClausePart, sortClause);

        Query query;
        if (isCountQuery) {
            query = entityManager.createNativeQuery(queryString);
        } else {
            query = entityManager.createNativeQuery(queryString, CaseDetailsEntity.class);
        }
        parametersToBind.forEach(query::setParameter);
        addParameters(query, criteria);
        log.debug("[SQL Query ]] : {}", queryString);
        return Optional.of(query);
    }

    private String secure(String clauses, MetaData metadata, Map<String, Object> params) {
        var userCaseAccessClause = addUserCaseAccessClause(params, metadata);
        var userCaseStateAccessClause = addUserCaseStateAccessClause(metadata, params);
        if (!userCaseAccessClause.isEmpty() || !userCaseStateAccessClause.isEmpty()) {
            return clauses + userCaseAccessClause + userCaseStateAccessClause;
        }
        return "";
    }

    private String addUserCaseAccessClause(Map<String, Object> params, MetaData metadata) {
        if (applicationParam.getEnableAttributeBasedAccessControl()) {
            CaseTypeDefinition caseTypeDefinition = caseTypeService
                .getCaseTypeForJurisdiction(metadata.getCaseTypeId(), metadata.getJurisdiction());
            List<RoleAssignment> roleAssignments = caseDataAccessControl.generateRoleAssignments(caseTypeDefinition);

            if (!roleAssignments.isEmpty()) {
                return accessControlGrantTypeQueryBuilder.createQuery(roleAssignments, params, caseTypeDefinition);
            }
        } else if (UserAuthorisation.AccessLevel.GRANTED.equals(userAuthorisation.getAccessLevel())) {
            params.put(USER_ID_PARAM, userAuthorisation.getUserId());
            return " AND id IN (SELECT cu.case_data_id FROM case_users AS cu WHERE user_id = :"
                + USER_ID_PARAM + ")";
        }
        return "";
    }

    private String addUserCaseStateAccessClause(MetaData metadata, Map<String, Object> params) {
        if (!applicationParam.getEnableAttributeBasedAccessControl()) {
            // restrict cases to the case states the user has access to
            List<String> caseStateIds =
                authorisedCaseDefinitionDataService.getUserAuthorisedCaseStateIds(metadata.getJurisdiction(),
                    metadata.getCaseTypeId(),
                    CAN_READ);
            if (!caseStateIds.isEmpty()) {
                params.put(CASE_STATES_PARAM, caseStateIds);
                return " AND state IN (:" + CASE_STATES_PARAM + ")";
            }
        }
        return "";
    }

    /**
     * Criteria are bound after the access control parameters, so a criterion whose parameter id matched an
     * access control parameter would silently replace that predicate's value and widen the user's access.
     * The reserved prefix makes this unreachable; this check keeps the guarantee enforced
     * if the field name validation or the prefix ever changes.
     */
    private void rejectAccessControlParameterCollisions(List<Criterion> criteria,
                                                        Map<String, Object> accessControlParams) {
        criteria.stream()
            .map(Criterion::buildParameterId)
            .filter(accessControlParams::containsKey)
            .findFirst()
            .ifPresent(parameterId -> {
                // This is a security risk, so we should log it and throw an exception.
                log.warn("Search field parameter '{}' collides with an access control parameter", parameterId);
                throw new BadRequestException("Field Names Invalid");
            });
    }

    private void addParameters(final Query query, List<Criterion> criteria) {
        criteria.forEach(criterion -> query.setParameter(criterion.buildParameterId(), criterion.getSoughtValue()));
    }

    private String toClauses(final List<Criterion> criteria) {
        return criteria.stream()
            .map(criterion -> criterion.buildClauseString(getOperation()))
            .collect(Collectors.joining(AND));
    }

    private String getOperation() {
        return this.applicationParam.isWildcardSearchAllowed() ? OPERATION_LIKE : OPERATION_EQ;
    }

}
