package uk.gov.hmcts.ccd.data.casedetails.search;

import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Named
@Singleton
public class SearchQueryFactoryOperation {

    private static final String AND = " AND ";
    private static final String OPERATION_EQ = " = ";
    private static final String OPERATION_LIKE = " LIKE ";

    @PersistenceContext
    private EntityManager entityManager;

    private static final String MAINQUERY = "SELECT * FROM case_data WHERE %s ORDER BY created_date ASC";
    private static final String MAIN_COUNT_QUERY = "SELECT count(*) FROM case_data WHERE %s";

    private final CriterionFactory criteraFactory;

    private final ApplicationParams applicationParam;

    public SearchQueryFactoryOperation(CriterionFactory criteraFactory,
            FieldMapSanitizeOperation fieldMapSanitizeOperation,
            final EntityManager entityManager,
            ApplicationParams applicationParam) {
        this.criteraFactory = criteraFactory;
        this.entityManager = entityManager;
        this.applicationParam = applicationParam;
    }

    public Query build(MetaData metadata, Map<String, String> params, boolean isCountQuery) {
        final List<Criterion> criteria = criteraFactory.build(metadata, params);
        String queryString = String.format(isCountQuery ? MAIN_COUNT_QUERY : MAINQUERY, toClauses(criteria));
        Query query;
        if (isCountQuery) {
            query = entityManager.createNativeQuery(queryString);
        } else {
             query = entityManager.createNativeQuery(queryString, CaseDetailsEntity.class);
        }
        addParameters(query, criteria);
        return query;
    }

    private void addParameters(final Query query, List<Criterion> critereon) {

        IntStream.range(0, critereon.size())
                .forEach(position -> query.setParameter(position, critereon.get(position).getSoughtValue()));
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
