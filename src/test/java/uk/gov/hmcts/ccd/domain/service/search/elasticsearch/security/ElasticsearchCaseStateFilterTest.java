package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security;

import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.STATE_FIELD_COL;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.service.security.AuthorisedCaseDefinitionDataService;

class ElasticsearchCaseStateFilterTest {

    @Mock
    private AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService;

    @InjectMocks
    private ElasticsearchCaseStateFilter filter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void shouldCreateTermsQueryBuilder() {
        String caseTypeId = "caseType";
        String state = "SomeState";
        when(authorisedCaseDefinitionDataService.getUserAuthorisedCaseStateIds(caseTypeId, CAN_READ))
                .thenReturn(Collections.singletonList(state));

        Optional<QueryBuilder> optQueryBuilder = filter.getFilter(caseTypeId);

        assertThat(optQueryBuilder.isPresent(), is(true));
        assertThat(optQueryBuilder.get(), instanceOf(TermsQueryBuilder.class));
        TermsQueryBuilder queryBuilder = (TermsQueryBuilder) optQueryBuilder.get();
        assertThat(queryBuilder.fieldName(), is(STATE_FIELD_COL));
        assertThat(queryBuilder.values(), hasItem(state.toLowerCase()));
    }
}
