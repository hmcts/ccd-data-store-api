package uk.gov.hmcts.ccd.domain.service.caseclosed;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.data.caseclosed.DateCaseClosedEntity;
import uk.gov.hmcts.ccd.data.caseclosed.DateCaseClosedRepository;
import uk.gov.hmcts.ccd.domain.model.search.DateCaseClosedResponse;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultClosedCaseSearchOperationTest {

    private static final Instant CLOSED_CASES_INSTANT = Instant.parse("2026-05-18T10:15:30Z");
    private static final Date CLOSED_CASES_DATE = Date.from(CLOSED_CASES_INSTANT);
    private static final LocalDateTime STATE_CHANGED_DATE =
        LocalDateTime.ofInstant(CLOSED_CASES_INSTANT, ZoneOffset.UTC);

    @Mock
    private DateCaseClosedRepository dateCaseClosedRepository;

    @InjectMocks
    private DefaultClosedCaseSearchOperation defaultClosedCaseSearchOperation;

    @Test
    void shouldReturnClosedCaseReferences() {
        DateCaseClosedEntity firstClosedCase = createDateCaseClosedEntity(1234567890123456L);
        DateCaseClosedEntity secondClosedCase = createDateCaseClosedEntity(2345678901234567L);
        when(dateCaseClosedRepository.findByStateChangedDateLessThanEqual(STATE_CHANGED_DATE))
            .thenReturn(List.of(firstClosedCase, secondClosedCase));

        DateCaseClosedResponse response = defaultClosedCaseSearchOperation.execute(CLOSED_CASES_DATE);

        assertAll(
            () -> verify(dateCaseClosedRepository).findByStateChangedDateLessThanEqual(STATE_CHANGED_DATE),
            () -> assertThat(response.getCaseReferences(), is(List.of("1234567890123456", "2345678901234567")))
        );
    }

    @Test
    void shouldReturnNullCaseReferencesWhenNoClosedCasesFound() {
        when(dateCaseClosedRepository.findByStateChangedDateLessThanEqual(STATE_CHANGED_DATE))
            .thenReturn(Collections.emptyList());

        DateCaseClosedResponse response = defaultClosedCaseSearchOperation.execute(CLOSED_CASES_DATE);

        assertAll(
            () -> verify(dateCaseClosedRepository).findByStateChangedDateLessThanEqual(STATE_CHANGED_DATE),
            () -> assertThat(response.getCaseReferences(), is(nullValue()))
        );
    }

    private DateCaseClosedEntity createDateCaseClosedEntity(Long caseReference) {
        DateCaseClosedEntity dateCaseClosedEntity = new DateCaseClosedEntity();
        dateCaseClosedEntity.setCcdCaseNumber(caseReference);
        return dateCaseClosedEntity;
    }
}
