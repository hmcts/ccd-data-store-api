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
import uk.gov.hmcts.ccd.domain.service.security.AuthorisedCaseDefinitionDataService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.STATE_FIELD_COL;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

class ElasticsearchCaseStateFilterTest {

    @Mock
    private AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService;

    @Mock
    private ApplicationParams applicationParams;

    @InjectMocks
    private ElasticsearchCaseStateFilter filter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldCreateTermsQuery() {
        String caseTypeId = "caseType";
        String state = "SomeState";

        when(authorisedCaseDefinitionDataService.getUserAuthorisedCaseStateIds(caseTypeId, CAN_READ))
            .thenReturn(Collections.singletonList(state.toLowerCase())); // lowercase match

        Optional<Query> optQuery = filter.getFilter(caseTypeId);

        assertTrue(optQuery.isPresent());
        Query query = optQuery.get();

        assertThat(query._kind(), is(Query.Kind.Terms));

        TermsQuery termsQuery = query.terms();
        assertThat(termsQuery.field(), is(STATE_FIELD_COL));

        List<FieldValue> values = termsQuery.terms().value();
        assertTrue(values.stream().anyMatch(v -> state.toLowerCase().equals(v.stringValue())));
    }
}
