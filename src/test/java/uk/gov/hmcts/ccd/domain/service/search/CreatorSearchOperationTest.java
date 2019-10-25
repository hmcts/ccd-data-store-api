package uk.gov.hmcts.ccd.domain.service.search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;

public class CreatorSearchOperationTest {

    @Mock
    private SearchOperation searchOperation;

    @Mock
    private CaseAccessService caseAccessService;

    @InjectMocks
    private CreatorSearchOperation classUnderTest;

    @Before
    public void injectMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @DisplayName("Should return an empty list of cases if the SearchOperation returns null")
    public void searchOperationReturnsNullListOfCases_emptyListOfCasesReturned() {

        when(searchOperation.execute(any(), any())).thenReturn(
            null
        );

        MetaData metaData = new MetaData(null,null);
        Map map = new HashMap();

        List<CaseDetails> results = classUnderTest.execute(metaData, map);

        assertTrue(results.isEmpty());

        verify(searchOperation).execute(same(metaData), same(map));

        verifyNoMoreInteractions(searchOperation);
        verifyZeroInteractions(caseAccessService);

    }

    private ArgumentMatcher<CaseDetails> matchesCaseIn(final List<CaseDetails> candidatesToMatch) {

        return new ArgumentMatcher<CaseDetails>() {
            @Override
            public boolean matches(CaseDetails argument) {
                return candidatesToMatch.contains(argument);
            }
        };
    }

}
