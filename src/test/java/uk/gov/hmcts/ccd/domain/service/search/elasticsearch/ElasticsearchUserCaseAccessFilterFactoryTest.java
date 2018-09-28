package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.STATE_FIELD_COL;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;

class ElasticsearchUserCaseAccessFilterFactoryTest {

    @Mock
    private CaseAccessService caseAccessService;

    @InjectMocks
    private ElasticsearchUserCaseAccessFilterFactory factory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void shouldCreateTermsQueryBuilder() {
        String caseTypeId = "caseType";
        Long caseId = 100L;
        when(caseAccessService.getGrantedCaseIdsForRestrictedRoles()).thenReturn(Optional.of(singletonList(caseId)));

        Optional<QueryBuilder> optQueryBuilder = factory.create(caseTypeId);

        assertThat(optQueryBuilder.isPresent(), is(true));
        assertThat(optQueryBuilder.get(), instanceOf(TermsQueryBuilder.class));
        TermsQueryBuilder queryBuilder = (TermsQueryBuilder) optQueryBuilder.get();
        assertThat(queryBuilder.fieldName(), is(STATE_FIELD_COL));
        assertThat(queryBuilder.values(), hasItem(caseId));
    }

}
