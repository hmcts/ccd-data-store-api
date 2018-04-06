package uk.gov.hmcts.ccd.domain.service.search;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;

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
    @DisplayName("Should call the CreatorVisibilityService for each case returned from the SearchOperation and return those "
        + "for which the CreatorVisibilityService returns true")
    public void searchOperationReturnsListOfCases_solicitorVisibilityServiceCalledForEachCase_listOfVisibleCasesReturned() {

        CaseDetails visibleCase1 = new CaseDetails();
        CaseDetails invisibleCase1 = new CaseDetails();
        CaseDetails invisibleCase2 = new CaseDetails();
        CaseDetails visibleCase2 = new CaseDetails();
        CaseDetails invisibleCase3 = new CaseDetails();
        CaseDetails visibleCase3 = new CaseDetails();

        when(searchOperation.execute(any(), any())).thenReturn(
            Arrays.asList(visibleCase1,invisibleCase1,invisibleCase2,
                visibleCase2, invisibleCase3, visibleCase3)
        );

        when(caseAccessService
                .canUserAccess(argThat(matchesCaseIn(Arrays.asList(visibleCase1, visibleCase2, visibleCase3)))))
            .thenReturn(true);
        when(caseAccessService
            .canUserAccess(argThat(matchesCaseIn(Arrays.asList(invisibleCase1, invisibleCase2, invisibleCase3)))))
            .thenReturn(false);

        MetaData metaData = new MetaData(null,null);
        Map map = new HashMap();

        List<CaseDetails> results = classUnderTest.execute(metaData, map);

        assertEquals(3, results.size());
        assertThat(results, allOf(
            hasItem(visibleCase1),
            hasItem(visibleCase2),
            hasItem(visibleCase3)
            )
        );

        verify(searchOperation).execute(same(metaData), same(map));

        verify(caseAccessService).canUserAccess(same(visibleCase1));
        verify(caseAccessService).canUserAccess(same(invisibleCase1));
        verify(caseAccessService).canUserAccess(same(invisibleCase2));
        verify(caseAccessService).canUserAccess(same(visibleCase2));
        verify(caseAccessService).canUserAccess(same(invisibleCase3));
        verify(caseAccessService).canUserAccess(same(visibleCase3));

        verifyNoMoreInteractions(searchOperation);
        verifyNoMoreInteractions(caseAccessService);

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

    private Matcher<CaseDetails> matchesCaseIn(final List<CaseDetails> candidatesToMatch) {

        return new BaseMatcher<CaseDetails>() {
            @Override
            public boolean matches(Object o) {
                return o instanceof CaseDetails
                    && candidatesToMatch.contains(o);
            }

            @Override
            public void describeTo(Description description) {

            }
        };
    }

}
