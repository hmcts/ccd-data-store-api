package uk.gov.hmcts.ccd.data.casedetails.query;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;

@DisplayName("CaseDetailsQueryBuilderFactory")
class CaseDetailsQueryBuilderFactoryTest {

    @Mock
    private UserAuthorisationSecurity userAuthorisationSecurity;

    @Mock
    private EntityManager em;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private CriteriaQuery<CaseDetailsEntity> query;

    @Mock
    private CriteriaQuery<Long> countQuery;

    @Mock
    private Root<CaseDetailsEntity> root;

    @Mock
    private CaseStateAuthorisationSecurity caseStateAuthorisationSecurity;

    private CaseDetailsQueryBuilderFactory factory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        factory = new CaseDetailsQueryBuilderFactory(asList(caseStateAuthorisationSecurity, userAuthorisationSecurity));

        when(em.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(criteriaBuilder.createQuery(CaseDetailsEntity.class)).thenReturn(query);
        when(criteriaBuilder.createQuery(Long.class)).thenReturn(countQuery);
        when(query.from(CaseDetailsEntity.class)).thenReturn(root);
    }

    @Nested
    @DisplayName("create()")
    class Create {
        @Test
        @DisplayName("should secure new builder instance with user authorisation")
        void secureNewInstance() {
            final CaseDetailsQueryBuilder<CaseDetailsEntity> queryBuilder = factory.select(em);

            assertAll(
                () -> assertThat(queryBuilder, is(notNullValue())),
                () -> verify(userAuthorisationSecurity).secure(queryBuilder, null),
                () -> verify(caseStateAuthorisationSecurity).secure(queryBuilder, null)
            );
        }
    }

    @Nested
    @DisplayName("select()")
    class Select {
        @Test
        @DisplayName("should secure select query")
        void shouldSecureSelectQuery() {
            MetaData metaData = new MetaData("caseType", "jurisdiction");
            CaseDetailsQueryBuilder<CaseDetailsEntity> queryBuilder = factory.select(em, metaData);
            assertAll(
                () -> assertThat(queryBuilder, is(notNullValue())),
                () -> verify(userAuthorisationSecurity).secure(queryBuilder, metaData),
                () -> verify(caseStateAuthorisationSecurity).secure(queryBuilder, metaData)
            );
        }

        @Test
        @DisplayName("should add order by clause to select query")
        void shouldAddOrderByClauseToSelectQuery() {
            String sortDirection = "desc";
            MetaData metaData = new MetaData("caseType", "jurisdiction");
            metaData.setSortDirection(Optional.of(sortDirection));

            CaseDetailsQueryBuilder<CaseDetailsEntity> queryBuilder = factory.select(em, metaData);
            queryBuilder.orderBy(metaData);

            assertAll(
                () -> assertThat(queryBuilder, is(notNullValue())),
                () -> verify(root).get("createdDate"),
                () -> verify(criteriaBuilder).desc(any())
            );
        }
    }

    @Nested
    @DisplayName("count()")
    class Count {
        @Test
        @DisplayName("should secure count query")
        void shouldSecureCountQuery() {
            MetaData metaData = new MetaData("caseType", "jurisdiction");
            CaseDetailsQueryBuilder<Long> queryBuilder = factory.count(em, metaData);

            assertAll(
                () -> assertThat(queryBuilder, is(notNullValue())),
                () -> verify(userAuthorisationSecurity).secure(queryBuilder, metaData),
                () -> verify(caseStateAuthorisationSecurity).secure(queryBuilder, metaData)
            );
        }
    }
}
