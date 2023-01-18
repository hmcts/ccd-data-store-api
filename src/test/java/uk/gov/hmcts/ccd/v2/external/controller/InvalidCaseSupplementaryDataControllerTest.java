package uk.gov.hmcts.ccd.v2.external.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.service.supplementarydata.InvalidSupplementaryDataOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.external.domain.InvalidCaseSupplementaryDataRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvalidCaseSupplementaryDataControllerTest {

    public static final LocalDateTime DATE_10_DAYS_AGO = LocalDateTime.now().minusDays(10);
    public static final LocalDateTime DATE_5_DAYS_AHEAD = LocalDateTime.now().plusDays(5);
    public static final Integer DEFAULT_LIMIT = 10;
    public static final String CASE_ID = "123";

    @Mock
    private InvalidSupplementaryDataOperation invalidSupplementaryDataOperation;

    @Mock
    private InvalidCaseSupplementaryDataRequest request;

    private InvalidCaseSupplementaryDataController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(request.getDateFrom()).thenReturn(DATE_10_DAYS_AGO);
        when(request.getDateTo()).thenReturn(Optional.of(DATE_5_DAYS_AHEAD));
        when(request.getLimit()).thenReturn(DEFAULT_LIMIT);

        controller = new InvalidCaseSupplementaryDataController(invalidSupplementaryDataOperation);
    }

    @Test
    void shouldProcessValidRequest() {
        List<String> cases = List.of(CASE_ID);
        doReturn(cases).when(invalidSupplementaryDataOperation).getInvalidSupplementaryDataCases(
            DATE_10_DAYS_AGO, Optional.of(DATE_5_DAYS_AHEAD), DEFAULT_LIMIT
        );

        List<String> result = controller.getInvalidSupplementaryData(request);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(CASE_ID, result.get(0));

        verify(invalidSupplementaryDataOperation, times(1)).getInvalidSupplementaryDataCases(
            DATE_10_DAYS_AGO, Optional.of(DATE_5_DAYS_AHEAD), DEFAULT_LIMIT);
    }

    @Test
    void shouldThrowExceptionWhenDateFromIsNull() {
        when(request.getDateFrom()).thenReturn(null);

        BadRequestException exception = assertThrows(BadRequestException.class,
            () -> controller.getInvalidSupplementaryData(request));

        assertAll(
            () -> assertThat(exception.getMessage(),
                containsString("Invalid parameters: 'date_from' has to be defined"))
        );
    }

    @Test
    void shouldThrowExceptionWhenDateFromIsAfterDateTo() {
        when(request.getDateFrom()).thenReturn(DATE_5_DAYS_AHEAD);
        when(request.getDateTo()).thenReturn(Optional.of(DATE_10_DAYS_AGO));

        BadRequestException exception = assertThrows(BadRequestException.class,
            () -> controller.getInvalidSupplementaryData(request));

        assertAll(
            () -> assertThat(exception.getMessage(),
                containsString("Invalid parameters: 'date_from' has to be before 'date_to'"))
        );
    }
}
