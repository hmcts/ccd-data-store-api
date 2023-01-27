package uk.gov.hmcts.ccd.v2.external.controller;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;
import uk.gov.hmcts.ccd.domain.service.cauroles.CaseAssignedUserRolesOperation;
import uk.gov.hmcts.ccd.domain.service.supplementarydata.InvalidSupplementaryDataOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.external.domain.InvalidCaseSupplementaryDataRequest;
import uk.gov.hmcts.ccd.v2.external.domain.InvalidCaseSupplementaryDataResponse;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InvalidCaseSupplementaryDataControllerTest {

    public static final LocalDateTime DATE_10_DAYS_AGO = LocalDateTime.now().minusDays(10);
    public static final LocalDateTime DATE_5_DAYS_AHEAD = LocalDateTime.now().plusDays(5);
    public static final Integer DEFAULT_LIMIT = 10;
    public static final String CASE_ID = "123";
    public static final String CASE_TYPE = "CASE_TYPE";


    @Mock
    private InvalidSupplementaryDataOperation invalidSupplementaryDataOperation;

    @Mock
    private CaseAssignedUserRolesOperation caseAssignedUserRolesOperation;

    @Mock
    private InvalidCaseSupplementaryDataRequest request;

    @Mock
    public CaseAssignedUserRole caseAssignedUserRole;

    @Mock
    private ApplicationParams applicationParams;

    private InvalidCaseSupplementaryDataController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(request.getDateFrom()).thenReturn(DATE_10_DAYS_AGO);
        when(request.getDateTo()).thenReturn(Optional.of(DATE_5_DAYS_AHEAD));
        when(request.getLimit()).thenReturn(DEFAULT_LIMIT);
        when(request.getSearchRas()).thenReturn(Boolean.TRUE);
        when(applicationParams.getInvalidSupplementaryDataCaseTypes()).thenReturn(Arrays.asList(CASE_TYPE));

        controller = new InvalidCaseSupplementaryDataController(applicationParams, invalidSupplementaryDataOperation,
            caseAssignedUserRolesOperation);
    }

    @Test
    void shouldProcessValidRequest() {
        List<String> cases = List.of(CASE_ID);
        doReturn(cases).when(invalidSupplementaryDataOperation).getInvalidSupplementaryDataCases(CASE_TYPE,
            DATE_10_DAYS_AGO, Optional.of(DATE_5_DAYS_AHEAD), DEFAULT_LIMIT
        );

        List<Long> casesAsLong = cases.stream().map(Long::parseLong).collect(Collectors.toList());
        List<String> userIds = Collections.emptyList();
        doReturn(createCaseAssignedUserRoles()).when(caseAssignedUserRolesOperation)
            .findCaseUserRoles(casesAsLong, userIds);

        ResponseEntity<InvalidCaseSupplementaryDataResponse> response = controller.getInvalidSupplementaryData(request);

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getCaseIds().size());
        assertEquals(CASE_ID, response.getBody().getCaseIds().get(0));
        assertEquals(1, response.getBody().getCaseAssignedUserRoles().size());
        assertEquals(caseAssignedUserRole, response.getBody().getCaseAssignedUserRoles().get(0));

        verify(invalidSupplementaryDataOperation, times(1)).getInvalidSupplementaryDataCases(CASE_TYPE,
            DATE_10_DAYS_AGO, Optional.of(DATE_5_DAYS_AHEAD), DEFAULT_LIMIT);
        verify(caseAssignedUserRolesOperation, times(1)).findCaseUserRoles(casesAsLong, userIds);
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

    @Test
    void shouldProcessValidRequestWhenDateToIsEmpty() {
        when(request.getDateTo()).thenReturn(Optional.empty());

        List<String> cases = List.of(CASE_ID);
        doReturn(cases).when(invalidSupplementaryDataOperation).getInvalidSupplementaryDataCases(CASE_TYPE,
            DATE_10_DAYS_AGO, Optional.empty(), DEFAULT_LIMIT
        );

        ResponseEntity<InvalidCaseSupplementaryDataResponse> response = controller.getInvalidSupplementaryData(request);

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getCaseIds().size());
        assertEquals(CASE_ID, response.getBody().getCaseIds().get(0));

        verify(invalidSupplementaryDataOperation, times(1)).getInvalidSupplementaryDataCases(CASE_TYPE,
            DATE_10_DAYS_AGO, Optional.empty(), DEFAULT_LIMIT);
    }

    @Test
    void shouldProcessValidRequestWhenSearchRasIsFalse() {
        when(request.getSearchRas()).thenReturn(Boolean.FALSE);

        List<String> cases = List.of(CASE_ID);
        doReturn(cases).when(invalidSupplementaryDataOperation).getInvalidSupplementaryDataCases(CASE_TYPE,
             DATE_10_DAYS_AGO, Optional.of(DATE_5_DAYS_AHEAD), DEFAULT_LIMIT
        );

        ResponseEntity<InvalidCaseSupplementaryDataResponse> response = controller.getInvalidSupplementaryData(request);

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getCaseIds().size());
        assertEquals(CASE_ID, response.getBody().getCaseIds().get(0));
        assertEquals(0, response.getBody().getCaseAssignedUserRoles().size());

        verify(invalidSupplementaryDataOperation, times(1)).getInvalidSupplementaryDataCases(CASE_TYPE,
             DATE_10_DAYS_AGO, Optional.of(DATE_5_DAYS_AHEAD), DEFAULT_LIMIT);
        verifyNoInteractions(caseAssignedUserRolesOperation);
    }

    private List<CaseAssignedUserRole> createCaseAssignedUserRoles() {
        List<CaseAssignedUserRole> userRoles = Lists.newArrayList();
        userRoles.add(caseAssignedUserRole);
        return userRoles;
    }
}
