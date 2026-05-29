package uk.gov.hmcts.ccd.domain.service.caseclosed;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.DateCaseClosedResponse;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorisedClosedCaseSearchOperationImplTest {

    private static final Instant CLOSED_CASES_INSTANT = Instant.parse("2026-05-18T00:00:00Z");
    private static final Date CLOSED_CASES_DATE = Date.from(CLOSED_CASES_INSTANT);

    @Mock
    private ClosedCaseSearchOperation closedCaseSearchOperation;

    @Mock
    private GetCaseOperation getCaseOperation;

    @InjectMocks
    private AuthorisedClosedCaseSearchOperationImpl authorisedClosedCaseSearchOperation;

    @Test
    void shouldReturnClosedCaseReferences() {
        when(closedCaseSearchOperation.execute(CLOSED_CASES_DATE))
            .thenReturn(new DateCaseClosedResponse(List.of("1234567890123456", "2345678901234567")));
        when(getCaseOperation.execute("1234567890123456")).thenReturn(Optional.of(new CaseDetails()));
        when(getCaseOperation.execute("2345678901234567")).thenReturn(Optional.of(new CaseDetails()));

        DateCaseClosedResponse response = authorisedClosedCaseSearchOperation.execute(CLOSED_CASES_DATE);

        assertAll(
            () -> verify(closedCaseSearchOperation).execute(CLOSED_CASES_DATE),
            () -> verify(getCaseOperation).execute("1234567890123456"),
            () -> verify(getCaseOperation).execute("2345678901234567"),
            () -> assertThat(response.getCaseReferences(), is(List.of("1234567890123456", "2345678901234567")))
        );
    }

    @Test
    void shouldThrowCaseNotFoundExceptionWhenNoClosedCasesFound() {
        when(closedCaseSearchOperation.execute(CLOSED_CASES_DATE))
            .thenReturn(new DateCaseClosedResponse(Collections.emptyList()));

        CaseNotFoundException exception = assertThrows(
            CaseNotFoundException.class,
            () -> authorisedClosedCaseSearchOperation.execute(CLOSED_CASES_DATE)
        );

        assertAll(
            () -> verify(closedCaseSearchOperation).execute(CLOSED_CASES_DATE),
            () -> verifyNoInteractions(getCaseOperation),
            () -> assertThat(exception.getMessage(), is("Case data not found"))
        );
    }

    @Test
    void shouldReturnOnlyClosedCaseReferencesUserHasPermissionToRead() {
        when(closedCaseSearchOperation.execute(CLOSED_CASES_DATE))
            .thenReturn(new DateCaseClosedResponse(List.of("1234567890123456", "2345678901234567")));
        when(getCaseOperation.execute("1234567890123456")).thenReturn(Optional.of(new CaseDetails()));
        when(getCaseOperation.execute("2345678901234567")).thenReturn(Optional.empty());

        DateCaseClosedResponse response = authorisedClosedCaseSearchOperation.execute(CLOSED_CASES_DATE);

        assertAll(
            () -> verify(closedCaseSearchOperation).execute(CLOSED_CASES_DATE),
            () -> verify(getCaseOperation).execute("1234567890123456"),
            () -> verify(getCaseOperation).execute("2345678901234567"),
            () -> assertThat(response.getCaseReferences(), is(List.of("1234567890123456")))
        );
    }

    @Test
    void shouldThrowCaseNotFoundExceptionWhenUserDoesNotHaveReadPermission() {
        when(closedCaseSearchOperation.execute(CLOSED_CASES_DATE))
            .thenReturn(new DateCaseClosedResponse(List.of("1234567890123456")));
        when(getCaseOperation.execute("1234567890123456")).thenReturn(Optional.empty());

        CaseNotFoundException exception = assertThrows(
            CaseNotFoundException.class,
            () -> authorisedClosedCaseSearchOperation.execute(CLOSED_CASES_DATE)
        );

        assertAll(
            () -> verify(closedCaseSearchOperation).execute(CLOSED_CASES_DATE),
            () -> verify(getCaseOperation).execute("1234567890123456"),
            () -> assertThat(exception.getMessage(), is("Case data not found"))
        );
    }
}
