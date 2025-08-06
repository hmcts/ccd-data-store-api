package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
        MockitoAnnotations.openMocks(this);
        when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);
    }

    @Test
    void shouldCreateTermsQuery() {
        String caseTypeId = "caseType";
        Long caseId = 100L;

        when(caseAccessService.getGrantedCaseReferencesForRestrictedRoles(caseTypeDefinition))
            .thenReturn(Optional.of(singletonList(caseId)));

        when(caseDefinitionRepository.getCaseType(caseTypeId))
            .thenReturn(caseTypeDefinition);

        Optional<Query> optQuery = filter.getFilter(caseTypeId);

        assertTrue(optQuery.isPresent());
        Query query = optQuery.get();

        assertThat(query._kind(), is(Query.Kind.Terms));

        TermsQuery termsQuery = query.terms();
        assertThat(termsQuery.field(), is(REFERENCE_FIELD_COL));

        List<FieldValue> values = termsQuery.terms().value();
        assertTrue(values.stream().anyMatch(v ->
            v._kind() == FieldValue.Kind.Long && caseId.equals(v.longValue())
        ));
        verify(caseAccessService).getGrantedCaseReferencesForRestrictedRoles(caseTypeDefinition);
    }

    @Test
    void shouldReturnOptionalEmptyWhenNoCaseTypeDefinitionFound() {
        String caseTypeId = "caseType";

        when(caseDefinitionRepository.getCaseType(caseTypeId))
            .thenReturn(null);

        Optional<Query> optQuery = filter.getFilter(caseTypeId);

        assertTrue(optQuery.isEmpty());

        verifyNoInteractions(caseAccessService);
    }
}
