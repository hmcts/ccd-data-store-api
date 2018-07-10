package uk.gov.hmcts.ccd.domain.service.search;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.PaginatedSearchMetadata;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;
import uk.gov.hmcts.ccd.domain.service.getdraft.GetDraftsOperation;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static uk.gov.hmcts.ccd.domain.model.draft.DraftBuilder.aDraft;

class PaginatedSearchMetaDataOperationTest {

    private static final String CTID = "CTID";
    private static final String JID = "JID";
    @Mock
    private GetDraftsOperation getDraftsOperation;
    @Mock
    private CaseDetailsRepository caseDetailsRepository;
    @Mock
    private ApplicationParams applicationParams;

    private PaginatedSearchMetaDataOperation paginatedSearchMetaDataOperation;

    private MetaData metadata = new MetaData(CTID, JID);
    private Map<String, String> criteria = Maps.newHashMap();
    private PaginatedSearchMetadata paginatedSearchMetadata = new PaginatedSearchMetadata();
    private List<Draft> drafts = Lists.newArrayList();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        paginatedSearchMetadata.setTotalResultsCount(60);
        paginatedSearchMetadata.setTotalPagesCount(3);
        drafts.add(aDraft().build());
        drafts.add(aDraft().build());
        drafts.add(aDraft().build());
        paginatedSearchMetaDataOperation = new PaginatedSearchMetaDataOperation(getDraftsOperation, caseDetailsRepository, applicationParams);
    }

    @Test
    void shouldReturnCorrectPaginationMetadata() {
        doReturn(drafts).when(getDraftsOperation).execute();
        doReturn(paginatedSearchMetadata).when(caseDetailsRepository).getPaginatedSearchMetadata(metadata, criteria);
        doReturn(20).when(applicationParams).getPaginationPageSize();

        PaginatedSearchMetadata paginatedSearchMetadata = paginatedSearchMetaDataOperation.execute(metadata, criteria);

        assertAll(
            () -> assertThat(paginatedSearchMetadata.getTotalResultsCount(), is(63)),
            () -> assertThat(paginatedSearchMetadata.getTotalPagesCount(), is(4))
        );
    }
}
