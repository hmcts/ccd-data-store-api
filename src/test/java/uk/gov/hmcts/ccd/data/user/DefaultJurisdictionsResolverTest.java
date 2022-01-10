package uk.gov.hmcts.ccd.data.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

class DefaultJurisdictionsResolverTest {

    @Mock
    private ApplicationParams applicationParams;
    @Mock
    private JurisdictionsResolver idamJurisdictionsResolver;
    @Mock
    private JurisdictionsResolver attributeBasedJurisdictionsResolver;
    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    private JurisdictionsResolver jurisdictionsResolver;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        jurisdictionsResolver = new DefaultJurisdictionsResolver(applicationParams,
            idamJurisdictionsResolver, attributeBasedJurisdictionsResolver);
    }

    @Test
    public void shouldDelegateToIdamJurisdictionsResolver() {
        List<String> jurisdictions = asList("divorce", "probate");
        given(idamJurisdictionsResolver.getJurisdictions()).willReturn(jurisdictions);
        given(applicationParams.getEnableAttributeBasedAccessControl()).willReturn(false);

        assertEquals(jurisdictions, jurisdictionsResolver.getJurisdictions());

        verify(idamJurisdictionsResolver).getJurisdictions();
        verifyZeroInteractions(attributeBasedJurisdictionsResolver);
    }

    @Test
    public void shouldDelegateToAttributeBasedJurisdictionsResolver() {
        List<String> jurisdictions = asList("divorce", "probate");
        given(attributeBasedJurisdictionsResolver.getJurisdictions()).willReturn(jurisdictions);
        given(applicationParams.getEnableAttributeBasedAccessControl()).willReturn(true);

        assertEquals(jurisdictions, jurisdictionsResolver.getJurisdictions());

        verify(attributeBasedJurisdictionsResolver).getJurisdictions();
        verify(idamJurisdictionsResolver).getJurisdictions();
    }
}
