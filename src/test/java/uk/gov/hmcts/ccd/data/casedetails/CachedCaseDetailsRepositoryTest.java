package uk.gov.hmcts.ccd.data.casedetails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.PaginatedSearchMetadata;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.*;

import static java.lang.String.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

class CachedCaseDetailsRepositoryTest {

    private final static long CASE_ID = 100000L;
    private final static long CASE_REFERENCE = 999999L;
    private final static String JURISDICTION_ID = "JeyOne";
    private final static String CASE_TYPE_ID = "CaseTypeOne";

    @Mock
    private CaseDetailsRepository caseDetailsRepository;

    private CaseDetails caseDetails;
    private List<CaseDetails> caseDetailsList;

    private MetaData metaData;
    private Map<String, String> dataSearchParams;
    private PaginatedSearchMetadata paginatedSearchMetadata;

    private CachedCaseDetailsRepository classUnderTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        caseDetails = new CaseDetails();
        caseDetails.setId(CASE_ID);
        caseDetails.setReference(CASE_REFERENCE);
        caseDetails.setJurisdiction(JURISDICTION_ID);
        caseDetails.setCaseTypeId(CASE_TYPE_ID);

        CaseDetails anotherCaseDetails = new CaseDetails();

        caseDetailsList = Arrays.asList(caseDetails, anotherCaseDetails);

        metaData = new MetaData(CASE_TYPE_ID, JURISDICTION_ID);
        metaData.setCaseReference(Optional.of(valueOf(CASE_REFERENCE)));

        dataSearchParams = new HashMap<>();

        paginatedSearchMetadata = new PaginatedSearchMetadata();

        classUnderTest = new CachedCaseDetailsRepository(caseDetailsRepository);
    }

    @Test
    @DisplayName("Should call set method of case details repository")
    void set() {
        doReturn(caseDetails).when(caseDetailsRepository).set(caseDetails);

        CaseDetails returned = classUnderTest.set(caseDetails);

        assertAll(
            () -> assertThat(returned, is(caseDetails)),
            () -> verify(caseDetailsRepository, times(1)).set(caseDetails)
        );
    }

    @Test
    @DisplayName("Should call lock method of case details repository")
    void lockCase() {
        doReturn(caseDetails).when(caseDetailsRepository).lockCase(CASE_REFERENCE);

        CaseDetails returned = classUnderTest.lockCase(CASE_REFERENCE);

        assertAll(
            () -> assertThat(returned, is(caseDetails)),
            () -> verify(caseDetailsRepository, times(1)).lockCase(CASE_REFERENCE)
        );
    }


    @Nested
    @DisplayName("Paginated search metadata")
    class getPaginatedSearchMetadata {
        @Test
        @DisplayName("should initially retrieve paginated search metadata from decorated repository")
        void getPaginatedSearchMetaData() {
            doReturn(paginatedSearchMetadata).when(caseDetailsRepository).getPaginatedSearchMetadata(metaData,
                dataSearchParams);

            PaginatedSearchMetadata returned = classUnderTest.getPaginatedSearchMetadata(metaData, dataSearchParams);

            assertAll(
                () -> assertThat(returned, is(paginatedSearchMetadata)),
                () -> verify(caseDetailsRepository, times(1)).getPaginatedSearchMetadata(metaData, dataSearchParams)
            );
        }

        @Test
        @DisplayName("should cache paginated search metadata for subsequent calls")
        void getPaginatedSearchMetaDataAgain() {
            doReturn(paginatedSearchMetadata).when(caseDetailsRepository).getPaginatedSearchMetadata(metaData,
                dataSearchParams);

            classUnderTest.getPaginatedSearchMetadata(metaData, dataSearchParams);

            verify(caseDetailsRepository, times(1)).getPaginatedSearchMetadata(metaData, dataSearchParams);

            PaginatedSearchMetadata newPaginatedSearchMetadata = new PaginatedSearchMetadata();
            doReturn(newPaginatedSearchMetadata).when(caseDetailsRepository).getPaginatedSearchMetadata(metaData,
                dataSearchParams);
            PaginatedSearchMetadata returned = classUnderTest.getPaginatedSearchMetadata(metaData, dataSearchParams);

            assertAll(
                () -> assertThat(returned, is(paginatedSearchMetadata)),
                () -> verifyNoMoreInteractions(caseDetailsRepository)
            );
        }
    }

    @Nested
    @DisplayName("Case details list by MetaData and FieldData")
    class getCaseDetailsByMetaDataAndFieldData {
        @Test
        @DisplayName("should initially retrieve case details list from decorated repository")
        void findByMetaDataAndFieldData() {
            doReturn(caseDetailsList).when(caseDetailsRepository).findByMetaDataAndFieldData(metaData,
                dataSearchParams);

            List<CaseDetails> returned = classUnderTest.findByMetaDataAndFieldData(metaData, dataSearchParams);

            assertAll(
                () -> assertThat(returned, is(caseDetailsList)),
                () -> verify(caseDetailsRepository, times(1)).findByMetaDataAndFieldData(metaData, dataSearchParams)
            );
        }

        @Test
        @DisplayName("should cache case details list for subsequent calls")
        void findByMetaDataAndFieldDataAgain() {
            doReturn(caseDetailsList).when(caseDetailsRepository).findByMetaDataAndFieldData(metaData,
                dataSearchParams);

            classUnderTest.findByMetaDataAndFieldData(metaData, dataSearchParams);

            verify(caseDetailsRepository, times(1)).findByMetaDataAndFieldData(metaData, dataSearchParams);

            List<CaseDetails> newCaseDetailsList = Arrays.asList(new CaseDetails(), new CaseDetails());
            doReturn(newCaseDetailsList).when(caseDetailsRepository).findByMetaDataAndFieldData(metaData,
                dataSearchParams);
            List<CaseDetails> returned = classUnderTest.findByMetaDataAndFieldData(metaData, dataSearchParams);

            assertAll(
                () -> assertThat(returned, is(caseDetailsList)),
                () -> verifyNoMoreInteractions(caseDetailsRepository)
            );
        }
    }

    @Nested
    @DisplayName("Case details by jurisdictionId, caseTypeId and caseReference")
    class getCaseDetailsByUniqueInfo {
        @Test
        @DisplayName("should initially retrieve case details from decorated repository")
        void findUniqueCase() {
            doReturn(caseDetails).when(caseDetailsRepository).findUniqueCase(JURISDICTION_ID, CASE_TYPE_ID, valueOf
                (CASE_REFERENCE));

            CaseDetails returned = classUnderTest.findUniqueCase(JURISDICTION_ID, CASE_TYPE_ID, valueOf
                (CASE_REFERENCE));

            assertAll(
                () -> assertThat(returned, is(caseDetails)),
                () -> verify(caseDetailsRepository, times(1)).findUniqueCase(JURISDICTION_ID, CASE_TYPE_ID, valueOf
                    (CASE_REFERENCE))
            );
        }

        @Test
        @DisplayName("should cache case details for subsequent calls")
        void findUniqueCaseAgain() {
            doReturn(caseDetails).when(caseDetailsRepository).findUniqueCase(JURISDICTION_ID, CASE_TYPE_ID, valueOf
                (CASE_REFERENCE));

            classUnderTest.findUniqueCase(JURISDICTION_ID, CASE_TYPE_ID, valueOf(CASE_REFERENCE));

            verify(caseDetailsRepository, times(1)).findUniqueCase(JURISDICTION_ID, CASE_TYPE_ID, valueOf
                (CASE_REFERENCE));

            doReturn(new CaseDetails()).when(caseDetailsRepository).findUniqueCase(JURISDICTION_ID, CASE_TYPE_ID,
                valueOf
                    (CASE_REFERENCE));
            CaseDetails returned = classUnderTest.findUniqueCase(JURISDICTION_ID, CASE_TYPE_ID, valueOf
                (CASE_REFERENCE));

            assertAll(
                () -> assertThat(returned, is(caseDetails)),
                () -> verifyNoMoreInteractions(caseDetailsRepository)
            );
        }
    }

    @Nested
    @DisplayName("Case details by Reference")
    class getCaseDetailsByReference {
        @Test
        @DisplayName("should initially retrieve case details from decorated repository")
        void findByReference() {
            doReturn(caseDetails).when(caseDetailsRepository).findByReference(CASE_REFERENCE);

            CaseDetails returned = classUnderTest.findByReference(CASE_REFERENCE);

            assertAll(
                () -> assertThat(returned, is(caseDetails)),
                () -> verify(caseDetailsRepository, times(1)).findByReference(CASE_REFERENCE)
            );
        }

        @Test
        @DisplayName("should cache case details for subsequent calls")
        void findByReferenceAgain() {
            doReturn(caseDetails).when(caseDetailsRepository).findByReference(CASE_REFERENCE);

            classUnderTest.findByReference(CASE_REFERENCE);

            verify(caseDetailsRepository, times(1)).findByReference(CASE_REFERENCE);

            doReturn(new CaseDetails()).when(caseDetailsRepository).findByReference(CASE_REFERENCE);
            CaseDetails returned = classUnderTest.findByReference(CASE_REFERENCE);

            assertAll(
                () -> assertThat(returned, is(caseDetails)),
                () -> verifyNoMoreInteractions(caseDetailsRepository)
            );
        }
    }

    @Nested
    @DisplayName("Case details by Id")
    class getCaseDetailsById {

        @Test
        @DisplayName("should initially retrieve case details from decorated repository")
        void findById() {
            doReturn(caseDetails).when(caseDetailsRepository).findById(CASE_ID);

            CaseDetails returned = classUnderTest.findById(CASE_ID);

            assertAll(
                () -> assertThat(returned, is(caseDetails)),
                () -> verify(caseDetailsRepository, times(1)).findById(CASE_ID)
            );
        }

        @Test
        @DisplayName("should cache case details for subsequent calls")
        void findByIdAgain() {
            doReturn(caseDetails).when(caseDetailsRepository).findById(CASE_ID);

            classUnderTest.findById(CASE_ID);

            verify(caseDetailsRepository, times(1)).findById(CASE_ID);

            doReturn(new CaseDetails()).when(caseDetailsRepository).findById(CASE_ID);
            CaseDetails returned = classUnderTest.findById(CASE_ID);

            assertAll(
                () -> assertThat(returned, is(caseDetails)),
                () -> verifyNoMoreInteractions(caseDetailsRepository)
            );
        }
    }
}
