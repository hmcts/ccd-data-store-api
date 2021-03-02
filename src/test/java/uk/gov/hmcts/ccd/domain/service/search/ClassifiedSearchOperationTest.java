package uk.gov.hmcts.ccd.domain.service.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

class ClassifiedSearchOperationTest {

    private static final String CASE_TYPE_ID = "GrantOnly";
    private static final String JURISDICTION_ID = "Probate";
    private static final String STATE_ID = "Issued";

    @Mock
    private SearchOperation searchOperation;

    @Mock
    private SecurityClassificationService classificationService;

    private ClassifiedSearchOperation classifiedSearchOperation;
    private MetaData metaData;
    private HashMap<String, String> criteria;
    private CaseDetails case1;
    private CaseDetails case2;
    private CaseDetails classifiedCase1;
    private CaseDetails classifiedCase2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        metaData = new MetaData(CASE_TYPE_ID, JURISDICTION_ID);
        criteria = new HashMap<>();

        case1 = new CaseDetails();
        case2 = new CaseDetails();

        doReturn(Arrays.asList(case1, case2)).when(searchOperation).execute(metaData, criteria);

        classifiedCase1 = new CaseDetails();
        classifiedCase2 = new CaseDetails();

        doReturn(Optional.of(classifiedCase1)).when(classificationService).applyClassification(case1);
        doReturn(Optional.of(classifiedCase2)).when(classificationService).applyClassification(case2);

        classifiedSearchOperation = new ClassifiedSearchOperation(searchOperation, classificationService);
    }

    @Test
    @DisplayName("should call decorated implementation")
    void shouldCallDecoratedImplementation() {
        classifiedSearchOperation.execute(metaData, criteria);

        verify(searchOperation).execute(metaData, criteria);
    }

    @Test
    @DisplayName("should return empty list when decorated returns null")
    void shouldReturnEmptyListWhenNullResult() {
        doReturn(null).when(searchOperation).execute(metaData, criteria);

        final List<CaseDetails> output = classifiedSearchOperation.execute(metaData, criteria);

        assertAll(
            () -> assertThat(output, is(notNullValue())),
            () -> assertThat(output, hasSize(0))
        );
    }

    @Test
    @DisplayName("should return classified search results")
    void shouldReturnClassifiedSearchResults() {
        final List<CaseDetails> output = classifiedSearchOperation.execute(metaData, criteria);

        assertAll(
            () -> assertThat(output, hasSize(2)),
            () -> assertThat(output, hasItems(classifiedCase1, classifiedCase2)),
            () -> verify(classificationService).applyClassification(case1),
            () -> verify(classificationService).applyClassification(case2)
        );
    }

    @Test
    @DisplayName("should remove cases with higher classification from search results")
    void shouldRemoveCaseHigherClassification() {
        doReturn(Optional.empty()).when(classificationService).applyClassification(case2);

        final List<CaseDetails> output = classifiedSearchOperation.execute(metaData, criteria);

        assertAll(
            () -> assertThat(output, hasSize(1)),
            () -> assertThat(output, hasItems(classifiedCase1))
        );
    }

}
