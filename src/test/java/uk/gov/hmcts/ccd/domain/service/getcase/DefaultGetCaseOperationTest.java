package uk.gov.hmcts.ccd.domain.service.getcase;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

class DefaultGetCaseOperationTest {

    private static final String CASE_REFERENCE = "1234123412341234";
    private static final Long CASE_REFERENCE_LONG = Long.valueOf(CASE_REFERENCE);
    private static final CaseDetails CASE_DETAILS = new CaseDetails();

    @Mock
    private CaseDetailsRepository caseDetailsRepository;

    @Mock
    private UIDService uidService;

    private DefaultGetCaseOperation getCaseOperation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(Boolean.TRUE).when(uidService).validateUID(CASE_REFERENCE);

        getCaseOperation = new DefaultGetCaseOperation(caseDetailsRepository, uidService);
    }


    @Nested
    @DisplayName("execute(caseReference)")
    class ExecuteCaseReference {

        @Test
        @DisplayName("should throw Bad Request exception when case reference is null")
        void shouldThrowBadRequestWhenReferenceNull() {
            doReturn(Boolean.FALSE).when(uidService).validateUID(null);

            assertThrows(BadRequestException.class, () -> getCaseOperation.execute(null));
        }

        @Test
        @DisplayName("should throw Bad Request exception when case reference is invalid")
        void shouldThrowBadRequestWhenReferenceInvalid() {
            doReturn(Boolean.FALSE).when(uidService).validateUID(CASE_REFERENCE);

            assertAll(
                () -> assertThrows(BadRequestException.class, () -> getCaseOperation.execute(CASE_REFERENCE)),
                () -> verify(uidService).validateUID(CASE_REFERENCE)
            );
        }

        @Test
        @DisplayName("should return empty optional when case does not exist")
        void shouldReturnEmptyWhenCaseNotExist() {
            doReturn(null).when(caseDetailsRepository).findByReference(CASE_REFERENCE_LONG);

            final Optional<CaseDetails> output = getCaseOperation.execute(CASE_REFERENCE);

            assertAll(
                () -> assertThat(output.isPresent(), is(false)),
                () -> verify(caseDetailsRepository).findByReference(CASE_REFERENCE_LONG)
            );
        }

        @Test
        @DisplayName("should return case details when case exists")
        void shouldReturnCaseWhenExists() {
            doReturn(CASE_DETAILS).when(caseDetailsRepository).findByReference(CASE_REFERENCE_LONG);

            final Optional<CaseDetails> output = getCaseOperation.execute(CASE_REFERENCE);

            assertAll(
                () -> assertThat(output.get(), sameInstance(CASE_DETAILS)),
                () -> verify(caseDetailsRepository).findByReference(CASE_REFERENCE_LONG)
            );
        }

    }
}
