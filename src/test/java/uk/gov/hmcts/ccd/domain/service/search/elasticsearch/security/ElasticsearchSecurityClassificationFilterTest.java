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
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity.SECURITY_CLASSIFICATION_FIELD_COL;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PRIVATE;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PUBLIC;

class ElasticsearchSecurityClassificationFilterTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CaseTypeService caseTypeService;

    @Mock
    private ApplicationParams applicationParams;

    @InjectMocks
    private ElasticsearchSecurityClassificationFilter filter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);
    }

    @Test
    void shouldCreateTermsQuery() {
        final String jurisdictionId = "jurisdiction";

        // Mock user classification
        when(userRepository.getHighestUserClassification(jurisdictionId)).thenReturn(PRIVATE);

        // Set up jurisdiction and case type
        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        JurisdictionDefinition jurisdictionDefinition = new JurisdictionDefinition();
        jurisdictionDefinition.setId(jurisdictionId);
        caseTypeDefinition.setJurisdictionDefinition(jurisdictionDefinition);

        String caseTypeId = "caseType";
        when(caseTypeService.getCaseType(caseTypeId)).thenReturn(caseTypeDefinition);

        // Execute filter
        Optional<Query> optQuery = filter.getFilter(caseTypeId);

        // Assertions
        assertTrue(optQuery.isPresent());

        Query query = optQuery.get();
        assertThat(query._kind(), is(Query.Kind.Terms));

        TermsQuery termsQuery = query.terms();
        assertThat(termsQuery.field(), is(SECURITY_CLASSIFICATION_FIELD_COL));

        List<FieldValue> values = termsQuery.terms().value();
        List<String> valueStrings = values.stream()
            .map(FieldValue::stringValue)
            .toList();

        assertThat(valueStrings.contains(PUBLIC.name()), is(true));
        assertThat(valueStrings.contains(PRIVATE.name()), is(true));

        // Verifications
        verify(userRepository).getHighestUserClassification(jurisdictionId);
        verify(caseTypeService).getCaseType(caseTypeId);
    }
}
