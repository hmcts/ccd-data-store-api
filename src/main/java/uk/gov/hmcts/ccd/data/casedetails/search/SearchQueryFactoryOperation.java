package uk.gov.hmcts.ccd.data.casedetails.search;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

import com.google.common.collect.Maps;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;
import uk.gov.hmcts.ccd.domain.service.security.AuthorisedCaseDefinitionDataService;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

@Named
@Singleton
public class SearchQueryFactoryOperation {

    private static final String AND = " AND ";
    private static final String OPERATION_EQ = " = ";
    private static final String OPERATION_LIKE = " LIKE ";

    @PersistenceContext
    private final EntityManager entityManager;

    private static final String MAIN_QUERY = "SELECT * FROM case_data WHERE %s ORDER BY %s";
    private static final String MAIN_COUNT_QUERY = "SELECT count(*) FROM case_data WHERE %s";

    private final CriterionFactory criterionFactory;
    private final ApplicationParams applicationParam;
    private final UserAuthorisation userAuthorisation;
    private final SortOrderQueryBuilder sortOrderQueryBuilder;
    private final AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService;

    public SearchQueryFactoryOperation(CriterionFactory criterionFactory,
                                       EntityManager entityManager,
                                       ApplicationParams applicationParam,
                                       UserAuthorisation userAuthorisation,
                                       SortOrderQueryBuilder sortOrderQueryBuilder,
                                       AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService) {
        this.criterionFactory = criterionFactory;
        this.entityManager = entityManager;
        this.applicationParam = applicationParam;
        this.userAuthorisation = userAuthorisation;
        this.sortOrderQueryBuilder = sortOrderQueryBuilder;
        this.authorisedCaseDefinitionDataService = authorisedCaseDefinitionDataService;
    }

    public Query build(MetaData metadata, Map<String, String> params, boolean isCountQuery) {
        final List<Criterion> criteria = criterionFactory.build(metadata, params);

        Map<String, Object> parametersToBind = Maps.newHashMap();
        String queryToFormat = isCountQuery ? MAIN_COUNT_QUERY : MAIN_QUERY;
        String whereClausePart = secure(toClauses(criteria), metadata, parametersToBind);
        String sortClause = sortOrderQueryBuilder.buildSortOrderClause(metadata);

        String queryString = String.format(queryToFormat, whereClausePart, sortClause);

        Query query;
        if (isCountQuery) {
            query = entityManager.createNativeQuery(queryString);
        } else {
            query = entityManager.createNativeQuery(queryString, CaseDetailsEntity.class);
        }
        parametersToBind.forEach((k, v) -> query.setParameter(k, v));
        addParameters(query, criteria);
        return query;
    }

    private String secure(String clauses, MetaData metadata, Map<String, Object> params) {
        return clauses + addUserCaseAccessClause(params) + addUserCaseStateAccessClause(metadata, params);
    }

    private String addUserCaseAccessClause(Map<String, Object> params) {
        if (UserAuthorisation.AccessLevel.GRANTED.equals(userAuthorisation.getAccessLevel())) {
            params.put("user_id", userAuthorisation.getUserId());
            return " AND id IN (SELECT cu.case_data_id FROM case_users AS cu WHERE user_id = :user_id)";
        }
        return "";
    }

    private String addUserCaseStateAccessClause(MetaData metadata, Map<String, Object> params) {
        // restrict cases to the case states the user has access to
        List<String> caseStateIds = authorisedCaseDefinitionDataService.getUserAuthorisedCaseStateIds(metadata.getJurisdiction(),
                                                                                                      metadata.getCaseTypeId(),
                                                                                                      CAN_READ);
        if (!caseStateIds.isEmpty()) {
            params.put("states", caseStateIds);
            return " AND state IN (:states)";
        }

        return "";
    }

    private void addParameters(final Query query, List<Criterion> criterion) {

        IntStream.range(0, criterion.size())
                .forEach(position -> query.setParameter(position, criterion.get(position).getSoughtValue()));
    }

    private String toClauses(final List<Criterion> criterion) {
        return IntStream.range(0, criterion.size())
                .mapToObj(Integer::new)
                .map(position -> criterion.get(position).buildClauseString(position, getOperation()))
                .collect(Collectors.joining(AND));
    }

    private String getOperation() {
        return this.applicationParam.isWildcardSearchAllowed() ? OPERATION_LIKE : OPERATION_EQ;
    }

}
