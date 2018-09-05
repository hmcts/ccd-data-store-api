package uk.gov.hmcts.ccd.data.casedetails.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private CaseDetailsAuthorisationSecurity caseDetailsAuthorisationSecurity;

    private CaseDetailsQueryBuilderFactory factory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        factory = new CaseDetailsQueryBuilderFactory(userAuthorisationSecurity, singletonList(caseDetailsAuthorisationSecurity));

        when(em.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(criteriaBuilder.createQuery(CaseDetailsEntity.class)).thenReturn(query);
        when(criteriaBuilder.createQuery(Long.class)).thenReturn(countQuery);
        when(query.from(CaseDetailsEntity.class)).thenReturn(root);

        // Return argument as is
        when(userAuthorisationSecurity.secure(Matchers.any(CaseDetailsQueryBuilder.class)))
            .then((Answer<CaseDetailsQueryBuilder>) invocation -> (CaseDetailsQueryBuilder) invocation.getArguments()[0]);
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
                () -> verify(userAuthorisationSecurity).secure(queryBuilder)
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
                () -> verify(userAuthorisationSecurity).secure(queryBuilder),
                () -> verify(caseDetailsAuthorisationSecurity).secure(queryBuilder, metaData)
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
                () -> verify(userAuthorisationSecurity).secure(queryBuilder),
                () -> verify(caseDetailsAuthorisationSecurity).secure(queryBuilder, metaData)
            );
        }
    }
}
