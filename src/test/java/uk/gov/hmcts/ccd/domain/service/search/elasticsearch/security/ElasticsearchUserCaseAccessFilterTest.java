package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;

import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.REFERENCE_FIELD_COL;

class ElasticsearchUserCaseAccessFilterTest {

    @Mock
    private CaseAccessService caseAccessService;

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @Mock
    private CaseTypeDefinition caseTypeDefinition;

    @Mock
    private ApplicationParams applicationParams;

    @InjectMocks
    private ElasticsearchUserCaseAccessFilter filter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);
    }

    @Test
    void shouldCreateTermsQueryBuilder() {
        String caseTypeId = "caseType";
        Long caseId = 100L;
        when(caseAccessService.getGrantedCaseReferencesForRestrictedRoles(caseTypeDefinition))
            .thenReturn(Optional.of(singletonList(caseId)));
        when(caseDefinitionRepository.getCaseType(caseTypeId))
            .thenReturn(caseTypeDefinition);

        Optional<QueryBuilder> optQueryBuilder = filter.getFilter(caseTypeId);

        assertTrue(optQueryBuilder.isPresent());
        assertThat(optQueryBuilder.get(), instanceOf(TermsQueryBuilder.class));
        TermsQueryBuilder queryBuilder = (TermsQueryBuilder) optQueryBuilder.get();
        assertThat(queryBuilder.fieldName(), is(REFERENCE_FIELD_COL));
        assertThat(queryBuilder.values(), hasItem(caseId));

        verify(caseAccessService).getGrantedCaseReferencesForRestrictedRoles(caseTypeDefinition);
    }

    @Test
    void shouldReturnOptionalEmptyWhenNoCaseTypeDefinitionFound() {
        String caseTypeId = "caseType";

        when(caseDefinitionRepository.getCaseType(caseTypeId))
            .thenReturn(null);

        Optional<QueryBuilder> optQueryBuilder = filter.getFilter(caseTypeId);

        assertTrue(optQueryBuilder.isEmpty());

        verifyZeroInteractions(caseAccessService);
    }

}
