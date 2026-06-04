package uk.gov.hmcts.ccd.domain.service.caseclosed;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.data.caseclosed.DateCaseClosedEntity;
import uk.gov.hmcts.ccd.data.caseclosed.DateCaseClosedRepository;
import uk.gov.hmcts.ccd.domain.model.search.DateCaseClosedResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultClosedCaseSearchOperationTest {

    private static final LocalDate CLOSED_CASES_DATE = LocalDate.of(2026, 5, 18);
    private static final LocalDateTime NEXT_DAY_START = LocalDateTime.of(2026, 5, 19, 0, 0);

    @Mock
    private DateCaseClosedRepository dateCaseClosedRepository;

    @InjectMocks
    private DefaultClosedCaseSearchOperation defaultClosedCaseSearchOperation;

    @Test
    void shouldReturnClosedCaseReferences() {
        DateCaseClosedEntity firstClosedCase = createDateCaseClosedEntity(1234567890123456L);
        DateCaseClosedEntity secondClosedCase = createDateCaseClosedEntity(2345678901234567L);
        when(dateCaseClosedRepository.findByStateChangedDateBefore(NEXT_DAY_START))
            .thenReturn(List.of(firstClosedCase, secondClosedCase));

        DateCaseClosedResponse response = defaultClosedCaseSearchOperation.execute(CLOSED_CASES_DATE);

        assertAll(
            () -> verify(dateCaseClosedRepository).findByStateChangedDateBefore(NEXT_DAY_START),
            () -> assertThat(response.getCaseReferences(), is(List.of("1234567890123456", "2345678901234567")))
        );
    }

    @Test
    void shouldReturnNullCaseReferencesWhenNoClosedCasesFound() {
        when(dateCaseClosedRepository.findByStateChangedDateBefore(NEXT_DAY_START))
            .thenReturn(Collections.emptyList());

        DateCaseClosedResponse response = defaultClosedCaseSearchOperation.execute(CLOSED_CASES_DATE);

        assertAll(
            () -> verify(dateCaseClosedRepository).findByStateChangedDateBefore(NEXT_DAY_START),
            () -> assertThat(response.getCaseReferences(), is(nullValue()))
        );
    }

    private DateCaseClosedEntity createDateCaseClosedEntity(Long caseReference) {
        DateCaseClosedEntity dateCaseClosedEntity = new DateCaseClosedEntity();
        dateCaseClosedEntity.setCcdCaseNumber(caseReference);
        return dateCaseClosedEntity;
    }
}
