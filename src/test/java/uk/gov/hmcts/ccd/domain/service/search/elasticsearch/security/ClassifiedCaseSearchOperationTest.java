package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationServiceImpl;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ClassifiedCaseSearchOperationTest {

    private ClassifiedCaseSearchOperation classifiedSearchOperation;
    private CaseDetails case1;
    private CaseDetails case2;
    private CaseDetails classifiedCase1;
    private CaseDetails classifiedCase2;

    @Mock
    private CaseSearchOperation searchOperation;

    @Mock
    private SecurityClassificationServiceImpl classificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        case1 = new CaseDetails();
        case2 = new CaseDetails();

        classifiedCase1 = new CaseDetails();
        classifiedCase2 = new CaseDetails();

        doReturn(Optional.of(classifiedCase1)).when(classificationService).applyClassification(case1);
        doReturn(Optional.of(classifiedCase2)).when(classificationService).applyClassification(case2);

        classifiedSearchOperation = new ClassifiedCaseSearchOperation(searchOperation, classificationService);
    }

    @Test
    @DisplayName("should call decorated implementation")
    void shouldCallDecoratedImplementation() {
        CrossCaseTypeSearchRequest crossCaseTypeSearchRequest = mock(CrossCaseTypeSearchRequest.class);
        classifiedSearchOperation.execute(crossCaseTypeSearchRequest, false);

        verify(searchOperation).execute(crossCaseTypeSearchRequest, false);
    }

    @Test
    @DisplayName("should return empty list when decorated returns null")
    void shouldReturnEmptyListWhenNullResult() {
        CrossCaseTypeSearchRequest crossCaseTypeSearchRequest = mock(CrossCaseTypeSearchRequest.class);
        doReturn(null).when(searchOperation).execute(crossCaseTypeSearchRequest, false);

        CaseSearchResult output = classifiedSearchOperation.execute(crossCaseTypeSearchRequest, false);

        assertAll(
            () -> assertThat(output, is(notNullValue())),
            () -> assertThat(output.getCases(), is(nullValue()))
        );
    }

    @Test
    @DisplayName("should return classified search results")
    void shouldReturnClassifiedSearchResults() {
        CrossCaseTypeSearchRequest crossCaseTypeSearchRequest = mock(CrossCaseTypeSearchRequest.class);
        List<CaseDetails> cases = Arrays.asList(case1, case2);
        CaseSearchResult caseSearchResult = new CaseSearchResult(Long.valueOf(cases.size()), cases);
        doReturn(caseSearchResult).when(searchOperation).execute(crossCaseTypeSearchRequest, false);

        final CaseSearchResult output = classifiedSearchOperation.execute(crossCaseTypeSearchRequest, false);

        assertAll(
            () -> assertThat(output.getCases(), hasSize(2)),
            () -> assertThat(output.getCases(), hasItems(classifiedCase1, classifiedCase2)),
            () -> verify(classificationService).applyClassification(case1),
            () -> verify(classificationService).applyClassification(case2)
        );
    }

    @Test
    @DisplayName("should remove cases with higher classification from search results")
    void shouldRemoveCaseHigherClassification() {
        CrossCaseTypeSearchRequest crossCaseTypeSearchRequest = mock(CrossCaseTypeSearchRequest.class);
        List<CaseDetails> cases = Arrays.asList(case1, case2);
        CaseSearchResult caseSearchResult = new CaseSearchResult(Long.valueOf(cases.size()), cases);
        doReturn(caseSearchResult).when(searchOperation).execute(crossCaseTypeSearchRequest, false);

        doReturn(Optional.empty()).when(classificationService).applyClassification(case2);

        final CaseSearchResult output = classifiedSearchOperation.execute(crossCaseTypeSearchRequest, false);

        assertAll(
            () -> assertThat(output.getCases(), hasSize(1)),
            () -> assertThat(output.getCases(), hasItems(classifiedCase1))
        );
    }

}
