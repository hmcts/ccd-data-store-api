package uk.gov.hmcts.ccd.data.casedetails.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CaseDetailsQueryBuilderFactory")
class CaseDetailsQueryBuilderFactoryTest {

    @Mock
    private UserAuthorisationSecurity security;

    @Mock
    private EntityManager em;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private CriteriaQuery<CaseDetailsEntity> query;

    @Mock
    private Root<CaseDetailsEntity> root;

    @InjectMocks
    private CaseDetailsQueryBuilderFactory factory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(em.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(criteriaBuilder.createQuery(CaseDetailsEntity.class)).thenReturn(query);
        when(query.from(CaseDetailsEntity.class)).thenReturn(root);

        // Return argument as is
        when(security.secure(Matchers.any(CaseDetailsQueryBuilder.class)))
            .then((Answer<CaseDetailsQueryBuilder>) invocation -> (CaseDetailsQueryBuilder) invocation.getArguments()[0]);
    }

    @Nested
    @DisplayName("create()")
    class Create {
        @Test
        @DisplayName("should secure new builder instance with user authorisation")
        void secureNewInstance() {
            final CaseDetailsQueryBuilder queryBuilder = factory.select(em);

            assertAll(
                () -> assertThat(queryBuilder, is(notNullValue())),
                () -> verify(security).secure(queryBuilder)
            );
        }
    }
}
