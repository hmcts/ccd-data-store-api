package uk.gov.hmcts.ccd.data.casedetails;

import static java.lang.String.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.PaginatedSearchMetadata;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class CachedCaseDetailsRepositoryTest {

    private static final long CASE_ID = 100000L;
    private static final long CASE_REFERENCE = 999999L;
    private static final String CASE_REFERENCE_STR = "1234123412341236";
    private static final String JURISDICTION_ID = "JeyOne";
    private static final String CASE_TYPE_ID = "CaseTypeOne";

    @Mock
    private CaseDetailsRepository caseDetailsRepository;

    private CaseDetails caseDetails;
    private List<CaseDetails> caseDetailsList;

    private MetaData metaData;
    private Map<String, String> dataSearchParams;
    private PaginatedSearchMetadata paginatedSearchMetadata;

    @InjectMocks
    private CachedCaseDetailsRepository cachedRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
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
    }

    @Test
    @DisplayName("Should call set method of case details repository")
    void set() {
        doReturn(caseDetails).when(caseDetailsRepository).set(caseDetails);

        CaseDetails returned = cachedRepository.set(caseDetails);

        assertAll(
            () -> assertThat(returned, is(caseDetails)),
            () -> verify(caseDetailsRepository, times(1)).set(caseDetails)
        );
    }

    @Nested
    @DisplayName("Paginated search metadata")
    class GetPaginatedSearchMetadata {
        @Test
        @DisplayName("should initially retrieve paginated search metadata from decorated repository")
        void getPaginatedSearchMetaData() {
            doReturn(paginatedSearchMetadata).when(caseDetailsRepository).getPaginatedSearchMetadata(metaData,
                dataSearchParams);

            PaginatedSearchMetadata returned = cachedRepository.getPaginatedSearchMetadata(metaData, dataSearchParams);

            assertAll(
                () -> assertThat(returned, is(paginatedSearchMetadata)),
                () -> verify(caseDetailsRepository, times(1))
                    .getPaginatedSearchMetadata(metaData, dataSearchParams)
            );
        }

        @Test
        @DisplayName("should cache paginated search metadata for subsequent calls")
        void getPaginatedSearchMetaDataAgain() {
            doReturn(paginatedSearchMetadata).when(caseDetailsRepository).getPaginatedSearchMetadata(metaData,
                dataSearchParams);

            cachedRepository.getPaginatedSearchMetadata(metaData, dataSearchParams);

            verify(caseDetailsRepository, times(1)).getPaginatedSearchMetadata(metaData,
                dataSearchParams);

            PaginatedSearchMetadata newPaginatedSearchMetadata = new PaginatedSearchMetadata();
            doReturn(newPaginatedSearchMetadata).when(caseDetailsRepository).getPaginatedSearchMetadata(metaData,
                dataSearchParams);
            PaginatedSearchMetadata returned = cachedRepository.getPaginatedSearchMetadata(metaData, dataSearchParams);

            assertAll(
                () -> assertThat(returned, is(paginatedSearchMetadata)),
                () -> verifyNoMoreInteractions(caseDetailsRepository)
            );
        }
    }

    @Nested
    @DisplayName("Case details list by MetaData and FieldData")
    class GetCaseDetailsByMetaDataAndFieldData {
        @Test
        @DisplayName("should initially retrieve case details list from decorated repository")
        void findByMetaDataAndFieldData() {
            doReturn(caseDetailsList).when(caseDetailsRepository).findByMetaDataAndFieldData(metaData,
                dataSearchParams);

            List<CaseDetails> returned = cachedRepository.findByMetaDataAndFieldData(metaData, dataSearchParams);

            assertAll(
                () -> assertThat(returned, is(caseDetailsList)),
                () -> verify(caseDetailsRepository, times(1))
                    .findByMetaDataAndFieldData(metaData, dataSearchParams)
            );
        }

        @Test
        @DisplayName("should cache case details list for subsequent calls")
        void findByMetaDataAndFieldDataAgain() {
            doReturn(caseDetailsList).when(caseDetailsRepository).findByMetaDataAndFieldData(metaData,
                dataSearchParams);

            cachedRepository.findByMetaDataAndFieldData(metaData, dataSearchParams);

            verify(caseDetailsRepository, times(1)).findByMetaDataAndFieldData(metaData,
                dataSearchParams);

            List<CaseDetails> newCaseDetailsList = Arrays.asList(new CaseDetails(), new CaseDetails());
            doReturn(newCaseDetailsList).when(caseDetailsRepository).findByMetaDataAndFieldData(metaData,
                dataSearchParams);
            List<CaseDetails> returned = cachedRepository.findByMetaDataAndFieldData(metaData, dataSearchParams);

            assertAll(
                () -> assertThat(returned, is(caseDetailsList)),
                () -> verifyNoMoreInteractions(caseDetailsRepository)
            );
        }
    }

    @Nested
    @DisplayName("Case details by jurisdictionId, caseTypeId and caseReference")
    class GetCaseDetailsByUniqueInfo {
        @Test
        @DisplayName("should initially retrieve case details from decorated repository")
        void findUniqueCase() {
            doReturn(caseDetails).when(caseDetailsRepository).findUniqueCase(JURISDICTION_ID, CASE_TYPE_ID,
                valueOf(CASE_REFERENCE));

            CaseDetails returned = cachedRepository.findUniqueCase(JURISDICTION_ID, CASE_TYPE_ID,
                valueOf(CASE_REFERENCE));

            assertAll(
                () -> assertThat(returned, is(caseDetails)),
                () -> verify(caseDetailsRepository, times(1)).findUniqueCase(JURISDICTION_ID,
                    CASE_TYPE_ID, valueOf(CASE_REFERENCE))
            );
        }

        @Test
        @DisplayName("should cache case details for subsequent calls")
        void findUniqueCaseAgain() {
            doReturn(caseDetails).when(caseDetailsRepository).findUniqueCase(JURISDICTION_ID, CASE_TYPE_ID,
                valueOf(CASE_REFERENCE));

            cachedRepository.findUniqueCase(JURISDICTION_ID, CASE_TYPE_ID, valueOf(CASE_REFERENCE));

            verify(caseDetailsRepository, times(1)).findUniqueCase(JURISDICTION_ID, CASE_TYPE_ID,
                valueOf(CASE_REFERENCE));

            doReturn(new CaseDetails()).when(caseDetailsRepository).findUniqueCase(JURISDICTION_ID, CASE_TYPE_ID,
                valueOf(CASE_REFERENCE));
            CaseDetails returned = cachedRepository.findUniqueCase(JURISDICTION_ID, CASE_TYPE_ID,
                valueOf(CASE_REFERENCE));

            assertAll(
                () -> assertThat(returned, is(caseDetails)),
                () -> verifyNoMoreInteractions(caseDetailsRepository)
            );
        }
    }

    @Nested
    @DisplayName("Case details by Reference")
    class GetCaseDetailsByReference {
        @Test
        @DisplayName("should initially retrieve case details from decorated repository")
        void findByReference() {
            doReturn(caseDetails).when(caseDetailsRepository).findByReference(CASE_REFERENCE);

            CaseDetails returned = cachedRepository.findByReference(CASE_REFERENCE);

            assertAll(
                () -> assertThat(returned, is(caseDetails)),
                () -> verify(caseDetailsRepository, times(1)).findByReference(CASE_REFERENCE)
            );
        }

        @Test
        @DisplayName("should cache case details for subsequent calls")
        void findByReferenceAgain() {
            doReturn(caseDetails).when(caseDetailsRepository).findByReference(CASE_REFERENCE);

            cachedRepository.findByReference(CASE_REFERENCE);

            verify(caseDetailsRepository, times(1)).findByReference(CASE_REFERENCE);

            doReturn(new CaseDetails()).when(caseDetailsRepository).findByReference(CASE_REFERENCE);
            CaseDetails returned = cachedRepository.findByReference(CASE_REFERENCE);

            assertAll(
                () -> assertThat(returned, is(caseDetails)),
                () -> verifyNoMoreInteractions(caseDetailsRepository)
            );
        }
    }

    @Nested
    @DisplayName("Case details by Id")
    class GetCaseDetailsById {

        @Test
        @DisplayName("should initially retrieve case details from decorated repository")
        void findById() {
            doReturn(caseDetails).when(caseDetailsRepository).findById(CASE_ID);

            CaseDetails returned = cachedRepository.findById(CASE_ID);

            assertAll(
                () -> assertThat(returned, is(caseDetails)),
                () -> verify(caseDetailsRepository, times(1)).findById(CASE_ID)
            );
        }

        @Test
        @DisplayName("should cache case details for subsequent calls")
        void findByIdAgain() {
            doReturn(caseDetails).when(caseDetailsRepository).findById(CASE_ID);

            cachedRepository.findById(CASE_ID);

            verify(caseDetailsRepository, times(1)).findById(CASE_ID);

            doReturn(new CaseDetails()).when(caseDetailsRepository).findById(CASE_ID);
            CaseDetails returned = cachedRepository.findById(CASE_ID);

            assertAll(
                () -> assertThat(returned, is(caseDetails)),
                () -> verifyNoMoreInteractions(caseDetailsRepository)
            );
        }
    }

    @Nested
    @DisplayName("findById(String, Long)")
    class FindByIdWithJurisdiction {
        @Test
        @DisplayName("should initially retrieve case details from decorated repository")
        void findById() {
            doReturn(Optional.of(caseDetails)).when(caseDetailsRepository).findById(JURISDICTION_ID, CASE_ID);

            final CaseDetails returned = cachedRepository.findById(JURISDICTION_ID, CASE_ID)
                .orElseThrow(() -> new AssertionError("Not found"));

            assertAll(
                () -> assertThat(returned, is(caseDetails)),
                () -> verify(caseDetailsRepository, times(1)).findById(JURISDICTION_ID, CASE_ID)
            );
        }

        @Test
        @DisplayName("should cache case details for subsequent calls")
        void findByIdAgain() {
            doReturn(Optional.of(caseDetails)).when(caseDetailsRepository).findById(JURISDICTION_ID, CASE_ID);

            cachedRepository.findById(JURISDICTION_ID, CASE_ID);

            verify(caseDetailsRepository, times(1)).findById(JURISDICTION_ID, CASE_ID);

            doReturn(Optional.of(new CaseDetails())).when(caseDetailsRepository).findById(JURISDICTION_ID, CASE_ID);

            final CaseDetails returned = cachedRepository.findById(JURISDICTION_ID, CASE_ID)
                .orElseThrow(() -> new AssertionError("Not found"));

            assertAll(
                () -> assertThat(returned, is(caseDetails)),
                () -> verifyNoMoreInteractions(caseDetailsRepository)
            );
        }
    }

    @Nested
    @DisplayName("findByReference(String, String)")
    class FindByReferenceAsString {
        @Test
        @DisplayName("should initially retrieve case details from decorated repository")
        void findByReference() {
            doReturn(Optional.of(caseDetails)).when(caseDetailsRepository).findByReference(JURISDICTION_ID,
                CASE_REFERENCE_STR);

            final CaseDetails returned = cachedRepository.findByReference(JURISDICTION_ID, CASE_REFERENCE_STR)
                .orElseThrow(() -> new AssertionError("Not found"));

            assertAll(
                () -> assertThat(returned, is(caseDetails)),
                () -> verify(caseDetailsRepository, times(1)).findByReference(JURISDICTION_ID,
                    CASE_REFERENCE_STR)
            );
        }

        @Test
        @DisplayName("should cache case details for subsequent calls")
        void findByReferenceAgain() {
            doReturn(Optional.of(caseDetails)).when(caseDetailsRepository).findByReference(JURISDICTION_ID,
                CASE_REFERENCE_STR);

            cachedRepository.findByReference(JURISDICTION_ID, CASE_REFERENCE_STR);

            verify(caseDetailsRepository, times(1)).findByReference(JURISDICTION_ID,
                CASE_REFERENCE_STR);

            doReturn(Optional.of(new CaseDetails())).when(caseDetailsRepository).findByReference(JURISDICTION_ID,
                CASE_REFERENCE_STR);

            final CaseDetails returned = cachedRepository.findByReference(JURISDICTION_ID, CASE_REFERENCE_STR)
                .orElseThrow(() -> new AssertionError("Not found"));

            assertAll(
                () -> assertThat(returned, is(caseDetails)),
                () -> verifyNoMoreInteractions(caseDetailsRepository)
            );
        }
    }

    @Nested
    @DisplayName("findByReference(String)")
    class FindByReferenceString {
        @Test
        @DisplayName("should initially retrieve case details from decorated repository")
        void findByReference() {
            doReturn(Optional.of(caseDetails)).when(caseDetailsRepository).findByReference(CASE_REFERENCE_STR);

            final CaseDetails returned = cachedRepository.findByReference(CASE_REFERENCE_STR)
                .orElseThrow(() -> new AssertionError("Not found"));

            assertAll(
                () -> assertThat(returned, is(caseDetails)),
                () -> verify(caseDetailsRepository, times(1)).findByReference(CASE_REFERENCE_STR)
            );
        }

        @Test
        @DisplayName("should cache case details for subsequent calls")
        void findByReferenceAgain() {
            doReturn(Optional.of(caseDetails)).when(caseDetailsRepository).findByReference(CASE_REFERENCE_STR);

            cachedRepository.findByReference(CASE_REFERENCE_STR);

            verify(caseDetailsRepository, times(1)).findByReference(CASE_REFERENCE_STR);

            doReturn(Optional.of(new CaseDetails())).when(caseDetailsRepository).findByReference(CASE_REFERENCE_STR);

            final CaseDetails returned = cachedRepository.findByReference(CASE_REFERENCE_STR)
                .orElseThrow(() -> new AssertionError("Not found"));

            assertAll(
                () -> assertThat(returned, is(caseDetails)),
                () -> verifyNoMoreInteractions(caseDetailsRepository)
            );
        }
    }

    @Nested
    @DisplayName("findByReferenceWithNoAccessControl(String)")
    class FindByReferenceWithNoAccessControl {
        @Test
        @DisplayName("should initially retrieve case details from decorated repository")
        void findByReferenceWithNoAccessControl() {
            doReturn(Optional.of(caseDetails)).when(caseDetailsRepository).findByReferenceWithNoAccessControl(
                CASE_REFERENCE_STR);

            final CaseDetails returned = cachedRepository.findByReferenceWithNoAccessControl(CASE_REFERENCE_STR)
                .orElseThrow(() -> new AssertionError("Not found"));

            assertAll(
                () -> assertThat(returned, is(caseDetails)),
                () -> verify(caseDetailsRepository, times(1))
                    .findByReferenceWithNoAccessControl(CASE_REFERENCE_STR)
            );
        }

        @Test
        @DisplayName("should cache case details for subsequent calls")
        void findByReferenceWithNoAccessControlAgain() {
            doReturn(Optional.of(caseDetails)).when(caseDetailsRepository).findByReferenceWithNoAccessControl(
                CASE_REFERENCE_STR);

            cachedRepository.findByReferenceWithNoAccessControl(CASE_REFERENCE_STR);

            verify(caseDetailsRepository, times(1)).findByReferenceWithNoAccessControl(
                CASE_REFERENCE_STR);

            doReturn(Optional.of(new CaseDetails())).when(caseDetailsRepository).findByReferenceWithNoAccessControl(
                CASE_REFERENCE_STR);

            final CaseDetails returned = cachedRepository.findByReferenceWithNoAccessControl(CASE_REFERENCE_STR)
                .orElseThrow(() -> new AssertionError("Not found"));

            assertAll(
                () -> assertThat(returned, is(caseDetails)),
                () -> verifyNoMoreInteractions(caseDetailsRepository)
            );
        }
    }
}
