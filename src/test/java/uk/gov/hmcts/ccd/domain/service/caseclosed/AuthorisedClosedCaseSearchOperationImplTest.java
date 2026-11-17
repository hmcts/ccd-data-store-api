package uk.gov.hmcts.ccd.domain.service.caseclosed;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.search.DateCaseClosedResponse;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ForbiddenException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.caseclosed.AuthorisedClosedCaseSearchOperationImpl.DISPOSER_PAYMENT_USER_ROLE;

@ExtendWith(MockitoExtension.class)
class AuthorisedClosedCaseSearchOperationImplTest {

    private static final LocalDate CLOSED_CASES_DATE = LocalDate.of(2026, 5, 18);

    @Mock
    private ClosedCaseSearchOperation closedCaseSearchOperation;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthorisedClosedCaseSearchOperationImpl authorisedClosedCaseSearchOperation;

    @Test
    void shouldReturnClosedCaseReferencesWhenUserHasDisposerPaymentUserRole() {
        when(userRepository.anyRoleEqualsTo(DISPOSER_PAYMENT_USER_ROLE)).thenReturn(true);
        when(closedCaseSearchOperation.execute(CLOSED_CASES_DATE))
            .thenReturn(new DateCaseClosedResponse(List.of("1234567890123456", "2345678901234567")));

        DateCaseClosedResponse response = authorisedClosedCaseSearchOperation.execute(CLOSED_CASES_DATE);

        assertAll(
            () -> verify(userRepository).anyRoleEqualsTo(DISPOSER_PAYMENT_USER_ROLE),
            () -> verify(closedCaseSearchOperation).execute(CLOSED_CASES_DATE),
            () -> assertThat(response.getCaseReferences(), is(List.of("1234567890123456", "2345678901234567")))
        );
    }

    @Test
    void shouldThrowForbiddenExceptionWhenUserDoesNotHaveDisposerPaymentUserRole() {
        when(userRepository.anyRoleEqualsTo(DISPOSER_PAYMENT_USER_ROLE)).thenReturn(false);

        assertThrows(
            ForbiddenException.class,
            () -> authorisedClosedCaseSearchOperation.execute(CLOSED_CASES_DATE)
        );

        assertAll(
            () -> verify(userRepository).anyRoleEqualsTo(DISPOSER_PAYMENT_USER_ROLE),
            () -> verifyNoInteractions(closedCaseSearchOperation)
        );
    }

    @Test
    void shouldThrowCaseNotFoundExceptionWhenNoClosedCasesFound() {
        when(userRepository.anyRoleEqualsTo(DISPOSER_PAYMENT_USER_ROLE)).thenReturn(true);
        when(closedCaseSearchOperation.execute(CLOSED_CASES_DATE))
            .thenReturn(new DateCaseClosedResponse(Collections.emptyList()));

        CaseNotFoundException exception = assertThrows(
            CaseNotFoundException.class,
            () -> authorisedClosedCaseSearchOperation.execute(CLOSED_CASES_DATE)
        );

        assertAll(
            () -> verify(userRepository).anyRoleEqualsTo(DISPOSER_PAYMENT_USER_ROLE),
            () -> verify(closedCaseSearchOperation).execute(CLOSED_CASES_DATE),
            () -> assertThat(exception.getMessage(), is("Case data not found"))
        );
    }
}
