package uk.gov.hmcts.ccd.data.casedetails.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CaseDetailsQueryBuilderFactory")
class CaseDetailsQueryBuilderFactoryTest {

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

    private final CaseDetailsQueryBuilderFactory factory = new CaseDetailsQueryBuilderFactory();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(em.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(criteriaBuilder.createQuery(CaseDetailsEntity.class)).thenReturn(query);
        when(criteriaBuilder.createQuery(Long.class)).thenReturn(countQuery);
        when(query.from(CaseDetailsEntity.class)).thenReturn(root);
    }

    @Nested
    @DisplayName("create()")
    class Create {
        @Test
        @DisplayName("should create new select query builder")
        void secureCreateSelectQueryBuilder() {
            final CaseDetailsQueryBuilder<CaseDetailsEntity> queryBuilder = factory.select(em);

            assertAll(
                () -> assertThat(queryBuilder, is(notNullValue())),
                () -> assertThat(queryBuilder, instanceOf(SelectCaseDetailsQueryBuilder.class)),
                () -> verify(em).getCriteriaBuilder(),
                () -> verify(criteriaBuilder).createQuery(CaseDetailsEntity.class),
                () -> verify(query).from(CaseDetailsEntity.class)
            );
        }
    }

    @Nested
    @DisplayName("count()")
    class Count {
        @Test
        @DisplayName("should create new count query builder")
        void shouldCreateCountQueryBuilder() {
            CaseDetailsQueryBuilder<Long> queryBuilder = factory.count(em);

            assertAll(
                () -> assertThat(queryBuilder, is(notNullValue())),
                () -> assertThat(queryBuilder, instanceOf(CountCaseDetailsQueryBuilder.class)),
                () -> verify(em).getCriteriaBuilder(),
                () -> verify(criteriaBuilder).createQuery(Long.class)
            );
        }
    }
}
