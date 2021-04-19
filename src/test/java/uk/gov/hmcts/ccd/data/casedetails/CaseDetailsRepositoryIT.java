package uk.gov.hmcts.ccd.data.casedetails;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.PaginatedSearchMetadata;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.Optional;
import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.HashMap;

import static java.lang.String.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureWireMock(port = 0)
@TestPropertySource(locations = "classpath:test.properties")
public class CaseDetailsRepositoryIT {

    private static final long CASE_ID = 100000L;
    private static final long CASE_ID1 = 200000L;
    private static final long CASE_REFERENCE = 999999L;
    private static final String JURISDICTION_ID = "JeyOne";
    private static final String CASE_TYPE_ID = "CaseTypeOne";

    @SpyBean
    private DefaultCaseDetailsRepository caseDetailsRepository;

    @Autowired
    private CachedCaseDetailsRepository cachedCaseDetailsRepository;

    private CaseDetails caseDetails;
    private List<CaseDetails> caseDetailsList;

    private MetaData metaData;
    private Map<String, String> dataSearchParams;
    private PaginatedSearchMetadata paginatedSearchMetadata;

    @Before
    public void setUp() {
        caseDetails = new CaseDetails();
        caseDetails.setId(valueOf(CASE_ID));
        caseDetails.setReference(CASE_REFERENCE);
        caseDetails.setJurisdiction(JURISDICTION_ID);
        caseDetails.setCaseTypeId(CASE_TYPE_ID);

        CaseDetails anotherCaseDetails = new CaseDetails();

        caseDetailsList = Arrays.asList(caseDetails, anotherCaseDetails);

        metaData = new MetaData(CASE_TYPE_ID, JURISDICTION_ID);
        metaData.setCaseReference(Optional.of(valueOf(CASE_REFERENCE)));

        dataSearchParams = new HashMap<>();

        paginatedSearchMetadata = new PaginatedSearchMetadata();

        doReturn(paginatedSearchMetadata).when(caseDetailsRepository)
            .getPaginatedSearchMetadata(metaData, dataSearchParams);
    }

    @Test
    public void getPaginatedSearchMetaDataAgain() {
        PaginatedSearchMetadata returned1 = cachedCaseDetailsRepository
            .getPaginatedSearchMetadata(metaData, dataSearchParams);

        assertAll(
            () -> assertThat(returned1.toString(), is(paginatedSearchMetadata.toString())),
            () -> verify(caseDetailsRepository, times(1))
                .getPaginatedSearchMetadata(metaData, dataSearchParams)
        );

        PaginatedSearchMetadata returned2 = cachedCaseDetailsRepository
            .getPaginatedSearchMetadata(metaData, dataSearchParams);

        assertAll(
            () -> assertThat(returned2.toString(), is(paginatedSearchMetadata.toString())),
            () -> verifyNoMoreInteractions(caseDetailsRepository)
        );
    }

    @Test
    public void findByMetaDataAndFieldDataAgain() {
        doReturn(caseDetailsList).when(caseDetailsRepository).findByMetaDataAndFieldData(metaData,
                dataSearchParams);

        cachedCaseDetailsRepository.findByMetaDataAndFieldData(metaData, dataSearchParams);

        verify(caseDetailsRepository, times(1)).findByMetaDataAndFieldData(metaData,
                dataSearchParams);

        List<CaseDetails> returned = cachedCaseDetailsRepository.findByMetaDataAndFieldData(metaData, dataSearchParams);

        assertAll(
            () -> assertThat(returned.size(), is(caseDetailsList.size())),
            () -> verifyNoMoreInteractions(caseDetailsRepository)
        );
    }

    @Test
    public void findUniqueCaseAgain() {
        doReturn(caseDetails).when(caseDetailsRepository).findUniqueCase(JURISDICTION_ID, CASE_TYPE_ID,
                valueOf(CASE_REFERENCE));

        cachedCaseDetailsRepository.findUniqueCase(JURISDICTION_ID, CASE_TYPE_ID, valueOf(CASE_REFERENCE));

        verify(caseDetailsRepository, times(1)).findUniqueCase(JURISDICTION_ID, CASE_TYPE_ID,
                valueOf(CASE_REFERENCE));

        CaseDetails returned = cachedCaseDetailsRepository.findUniqueCase(JURISDICTION_ID, CASE_TYPE_ID,
                valueOf(CASE_REFERENCE));

        assertAll(
            () -> assertThat(returned.toString(), is(caseDetails.toString())),
            () -> verifyNoMoreInteractions(caseDetailsRepository)
        );
    }

    @Test
    public void findByIdAgain() {
        doReturn(caseDetails).when(caseDetailsRepository).findById(CASE_ID);

        cachedCaseDetailsRepository.findById(CASE_ID);

        verify(caseDetailsRepository, times(1)).findById(CASE_ID);

        CaseDetails returned = cachedCaseDetailsRepository.findById(CASE_ID);

        assertAll(
            () -> assertThat(returned.toString(), is(caseDetails.toString())),
            () -> verifyNoMoreInteractions(caseDetailsRepository)
        );
    }

    @Test
    public void findByIdAgain1() {
        doReturn(Optional.of(caseDetails)).when(caseDetailsRepository).findById(JURISDICTION_ID, CASE_ID1);

        cachedCaseDetailsRepository.findById(JURISDICTION_ID, CASE_ID1);

        verify(caseDetailsRepository, times(1)).findById(JURISDICTION_ID, CASE_ID1);

        final CaseDetails returned = cachedCaseDetailsRepository.findById(JURISDICTION_ID, CASE_ID1)
            .orElseThrow(() -> new AssertionError("Not found"));

        assertAll(
            () -> assertThat(returned.toString(), is(caseDetails.toString())),
            () -> verifyNoMoreInteractions(caseDetailsRepository)
        );
    }

    @Test
    public void findByReferenceLongValue() {
        doReturn(caseDetails).when(caseDetailsRepository).findByReference(CASE_REFERENCE);

        CaseDetails returned = cachedCaseDetailsRepository.findByReference(CASE_REFERENCE);

        assertAll(
            () -> assertThat(returned.toString(), is(caseDetails.toString())),
            () -> verify(caseDetailsRepository, times(1)).findByReference(CASE_REFERENCE)
        );

        CaseDetails returned1 = cachedCaseDetailsRepository.findByReference(CASE_REFERENCE);

        assertAll(
            () -> assertThat(returned1.toString(), is(caseDetails.toString())),
            () -> verifyNoMoreInteractions(caseDetailsRepository)
        );
    }

    @Test
    public void findByReferenceAndJurisdiction() {
        String caseReference = "case1";
        doReturn(Optional.of(caseDetails)).when(caseDetailsRepository).findByReference(JURISDICTION_ID,
                caseReference);

        cachedCaseDetailsRepository.findByReference(JURISDICTION_ID, caseReference);

        verify(caseDetailsRepository, times(1)).findByReference(JURISDICTION_ID,
                caseReference);

        final CaseDetails returned = cachedCaseDetailsRepository.findByReference(JURISDICTION_ID, caseReference)
                .orElseThrow(() -> new AssertionError("Not found"));

        assertAll(
            () -> assertThat(returned.toString(), is(caseDetails.toString())),
            () -> verifyNoMoreInteractions(caseDetailsRepository)
        );
    }

    @Test
    public void findByReferenceString() {
        String caseReference = "case2";
        doReturn(Optional.of(caseDetails)).when(caseDetailsRepository).findByReference(caseReference);

        cachedCaseDetailsRepository.findByReference(caseReference);

        verify(caseDetailsRepository, times(1)).findByReference(caseReference);

        final CaseDetails returned = cachedCaseDetailsRepository.findByReference(caseReference)
                .orElseThrow(() -> new AssertionError("Not found"));

        assertAll(
            () -> assertThat(returned.toString(), is(caseDetails.toString())),
            () -> verifyNoMoreInteractions(caseDetailsRepository)
        );
    }

    @Test
    public void findByReferenceWithNoAccessControlAgain() {
        String caseReference = "case3";
        doReturn(Optional.of(caseDetails)).when(caseDetailsRepository).findByReferenceWithNoAccessControl(
                caseReference);

        cachedCaseDetailsRepository.findByReferenceWithNoAccessControl(caseReference);

        verify(caseDetailsRepository, times(1)).findByReferenceWithNoAccessControl(
                caseReference);

        final CaseDetails returned = cachedCaseDetailsRepository.findByReferenceWithNoAccessControl(caseReference)
                .orElseThrow(() -> new AssertionError("Not found"));

        assertAll(
            () -> assertThat(returned.toString(), is(caseDetails.toString())),
            () -> verifyNoMoreInteractions(caseDetailsRepository)
        );
    }
}
