package uk.gov.hmcts.ccd.domain.service.search;

import com.google.common.collect.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.PaginatedSearchMetadata;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;

class PaginatedSearchMetaDataOperationTest {

    private static final String CTID = "CTID";
    private static final String JID = "JID";
    @Mock
    private CaseDetailsRepository caseDetailsRepository;

    private PaginatedSearchMetaDataOperation paginatedSearchMetaDataOperation;

    private MetaData metadata = new MetaData(CTID, JID);
    private Map<String, String> criteria = Maps.newHashMap();
    private PaginatedSearchMetadata paginatedSearchMetadata = new PaginatedSearchMetadata();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        paginatedSearchMetadata.setTotalResultsCount(60);
        paginatedSearchMetadata.setTotalPagesCount(3);
        paginatedSearchMetaDataOperation = new PaginatedSearchMetaDataOperation(caseDetailsRepository);
    }

    @Test
    void shouldReturnCorrectPaginationMetadata() {
        doReturn(paginatedSearchMetadata).when(caseDetailsRepository).getPaginatedSearchMetadata(metadata, criteria);

        PaginatedSearchMetadata paginatedSearchMetadata = paginatedSearchMetaDataOperation.execute(metadata, criteria);

        assertAll(
            () -> assertThat(paginatedSearchMetadata.getTotalResultsCount(), is(60)),
            () -> assertThat(paginatedSearchMetadata.getTotalPagesCount(), is(3))
        );
    }
}
