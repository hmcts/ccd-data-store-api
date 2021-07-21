package uk.gov.hmcts.ccd.data.casedetails.query;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class CaseDetailsQueryBuilderTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private CriteriaQuery<CaseDetailsEntity> criteriaQuery;

    @Mock
    private Root<CaseDetailsEntity> root;

    @Mock
    private CriteriaBuilder.In in;

    private SelectCaseDetailsQueryBuilder caseDetailsQueryBuilder;

    private final List<Long> caseReferences = Arrays.asList(new Long[]{5353535L});

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(criteriaBuilder.createQuery(CaseDetailsEntity.class)).thenReturn(criteriaQuery);
        when(criteriaQuery.from(CaseDetailsEntity.class)).thenReturn(root);
        when(criteriaBuilder.in(any())).thenReturn(in);
        when(in.value(caseReferences)).thenReturn(in);
        caseDetailsQueryBuilder = new SelectCaseDetailsQueryBuilder(entityManager);

    }

    @Test
    void whereGrantedAccessOnlyForRA() {

        CaseDetailsQueryBuilder result = caseDetailsQueryBuilder.whereGrantedAccessOnlyForRA(caseReferences);
        assertNotNull(result);
        assertEquals(result, caseDetailsQueryBuilder);
    }
}
