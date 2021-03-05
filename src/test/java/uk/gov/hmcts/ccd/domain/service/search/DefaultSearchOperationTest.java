package uk.gov.hmcts.ccd.domain.service.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

class DefaultSearchOperationTest {

    private static final String CASE_TYPE_ID = "GrantOnly";
    private static final String JURISDICTION_ID = "Probate";
    private static final String STATE = "Issued";

    @Mock
    private CaseDetailsRepository caseDetailsRepository;

    private DefaultSearchOperation searchOperation;
    private MetaData metaData;
    private HashMap<String, String> criteria;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        searchOperation = new DefaultSearchOperation(caseDetailsRepository);

        metaData = new MetaData(CASE_TYPE_ID, JURISDICTION_ID);
        criteria = new HashMap<>();
    }

    @Test
    @DisplayName("should search against repository")
    void shouldSearchAgainstRepository() {
        final ArrayList<CaseDetails> results = new ArrayList<>();
        doReturn(results).when(caseDetailsRepository).findByMetaDataAndFieldData(metaData, criteria);

        final List<CaseDetails> output = searchOperation.execute(metaData, criteria);

        assertAll(
            () -> verify(caseDetailsRepository).findByMetaDataAndFieldData(metaData, criteria),
            () -> assertThat(output, sameInstance(results))
        );
    }

}
