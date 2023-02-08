package uk.gov.hmcts.ccd.v2.external.controller;

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
import uk.gov.hmcts.ccd.v2.external.domain.InvalidCaseSupplementaryDataItem;
import uk.gov.hmcts.ccd.v2.external.domain.InvalidCaseSupplementaryDataRequest;
import uk.gov.hmcts.ccd.v2.external.domain.InvalidCaseSupplementaryDataResponse;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InvalidCaseSupplementaryDataControllerTest {

    public static final LocalDateTime DATE_10_DAYS_AGO = LocalDateTime.now().minusDays(10);
    public static final LocalDateTime DATE_5_DAYS_AHEAD = LocalDateTime.now().plusDays(5);
    public static final Integer DEFAULT_LIMIT = 10;
    public static final Long CASE_ID = 123L;
    public static final Long CASE_ID2 = 124L;
    public static final String CASE_TYPE_ID = "CASE_TYPE";
    public static final String APPLICANT1 = "applicant1";
    public static final String RESPONDENT1 = "respondent1";
    public static final String CASE_ROLE = "caseRoleA";
    public static final String USER_ID = "89000";
    public static final List<String> CASE_TYPES = List.of(CASE_TYPE_ID);

    @Mock
    private InvalidSupplementaryDataOperation invalidSupplementaryDataOperation;

    @Mock
    private CaseAssignedUserRolesOperation caseAssignedUserRolesOperation;

    @Mock
    private InvalidCaseSupplementaryDataRequest request;

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
        when(applicationParams.getInvalidSupplementaryDataCaseTypes()).thenReturn(List.of("", CASE_TYPE_ID));

        controller = new InvalidCaseSupplementaryDataController(invalidSupplementaryDataOperation,
            caseAssignedUserRolesOperation, applicationParams);
    }

    @Test
    void shouldProcessValidRequest() {
        List<InvalidCaseSupplementaryDataItem> cases = getTwoDataItems();
        doReturn(cases).when(invalidSupplementaryDataOperation).getInvalidSupplementaryDataCases(
            CASE_TYPES, DATE_10_DAYS_AGO, Optional.of(DATE_5_DAYS_AHEAD), DEFAULT_LIMIT
        );

        List<Long> caseIdsAsLong = cases.stream().map(InvalidCaseSupplementaryDataItem::getCaseId)
            .collect(Collectors.toList());
        List<String> userIds = Collections.emptyList();
        doReturn(List.of(new CaseAssignedUserRole(CASE_ID2.toString(), USER_ID, CASE_ROLE)))
            .when(caseAssignedUserRolesOperation).findCaseUserRoles(caseIdsAsLong, userIds);

        ResponseEntity<InvalidCaseSupplementaryDataResponse> response = controller.getInvalidSupplementaryData(request);

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getDataItems().size());

        List<InvalidCaseSupplementaryDataItem> dataItems = response.getBody().getDataItems();
        Optional<InvalidCaseSupplementaryDataItem> first = dataItems.stream()
            .filter(e -> e.getCaseId().equals(CASE_ID)).findFirst();
        assertTrue(first.isPresent());
        Optional<InvalidCaseSupplementaryDataItem> second = dataItems.stream()
            .filter(e -> e.getCaseId().equals(CASE_ID2)).findFirst();
        assertTrue(second.isPresent());

        // no caseAssignedUserRoles present so data not enhanced
        assertNull(first.get().getUserId());
        assertNull(first.get().getCaseRole());

        // test it has been enhanced with UserId and CaseRole
        assertEquals(USER_ID, second.get().getUserId());
        assertEquals(CASE_ROLE, second.get().getCaseRole());

        verify(invalidSupplementaryDataOperation, times(1)).getInvalidSupplementaryDataCases(
            CASE_TYPES, DATE_10_DAYS_AGO, Optional.of(DATE_5_DAYS_AHEAD), DEFAULT_LIMIT);
        verify(caseAssignedUserRolesOperation, times(1)).findCaseUserRoles(caseIdsAsLong, userIds);
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

        List<InvalidCaseSupplementaryDataItem> cases = getTwoDataItems();
        doReturn(cases).when(invalidSupplementaryDataOperation).getInvalidSupplementaryDataCases(
            CASE_TYPES, DATE_10_DAYS_AGO, Optional.empty(), DEFAULT_LIMIT
        );

        doReturn(List.of()).when(caseAssignedUserRolesOperation)
            .findCaseUserRoles(anyList(), anyList());

        ResponseEntity<InvalidCaseSupplementaryDataResponse> response = controller.getInvalidSupplementaryData(request);

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getDataItems().size());

        List<InvalidCaseSupplementaryDataItem> dataItems = response.getBody().getDataItems();
        Optional<InvalidCaseSupplementaryDataItem> first = dataItems.stream()
            .filter(e -> e.getCaseId().equals(CASE_ID)).findFirst();
        assertTrue(first.isPresent());
        Optional<InvalidCaseSupplementaryDataItem> second = dataItems.stream()
            .filter(e -> e.getCaseId().equals(CASE_ID2)).findFirst();
        assertTrue(second.isPresent());

        // no caseAssignedUserRoles present so data not enhanced
        assertNull(first.get().getUserId());
        assertNull(first.get().getCaseRole());
        assertNull(second.get().getUserId());
        assertNull(second.get().getCaseRole());

        verify(invalidSupplementaryDataOperation, times(1)).getInvalidSupplementaryDataCases(
            CASE_TYPES, DATE_10_DAYS_AGO, Optional.empty(), DEFAULT_LIMIT);
    }

    @Test
    void shouldProcessValidRequestWhenSearchRasIsFalse() {
        when(request.getSearchRas()).thenReturn(Boolean.FALSE);

        List<InvalidCaseSupplementaryDataItem> cases = getTwoDataItems();
        doReturn(cases).when(invalidSupplementaryDataOperation).getInvalidSupplementaryDataCases(
            CASE_TYPES, DATE_10_DAYS_AGO, Optional.of(DATE_5_DAYS_AHEAD), DEFAULT_LIMIT
        );

        ResponseEntity<InvalidCaseSupplementaryDataResponse> response = controller.getInvalidSupplementaryData(request);

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getDataItems().size());

        List<InvalidCaseSupplementaryDataItem> dataItems = response.getBody().getDataItems();
        Optional<InvalidCaseSupplementaryDataItem> first = dataItems.stream()
            .filter(e -> e.getCaseId().equals(CASE_ID)).findFirst();
        assertTrue(first.isPresent());
        Optional<InvalidCaseSupplementaryDataItem> second = dataItems.stream()
            .filter(e -> e.getCaseId().equals(CASE_ID2)).findFirst();
        assertTrue(second.isPresent());

        // no caseAssignedUserRoles present so data not enhanced
        assertNull(first.get().getUserId());
        assertNull(first.get().getCaseRole());
        assertNull(second.get().getUserId());
        assertNull(second.get().getCaseRole());

        verify(invalidSupplementaryDataOperation, times(1)).getInvalidSupplementaryDataCases(
            CASE_TYPES, DATE_10_DAYS_AGO, Optional.of(DATE_5_DAYS_AHEAD), DEFAULT_LIMIT);
        verifyNoInteractions(caseAssignedUserRolesOperation);
    }

    private List<InvalidCaseSupplementaryDataItem> getTwoDataItems() {
        InvalidCaseSupplementaryDataItem dateItem1 = InvalidCaseSupplementaryDataItem.builder()
            .caseId(CASE_ID)
            .caseTypeId(CASE_TYPE_ID)
            .organisationPolicyOrgIds(List.of(APPLICANT1))
            .orgPolicyCaseAssignedRoles(List.of(CASE_ROLE))
            .build();

        InvalidCaseSupplementaryDataItem dateItem2 = InvalidCaseSupplementaryDataItem.builder()
            .caseId(CASE_ID2)
            .caseTypeId(CASE_TYPE_ID)
            .organisationPolicyOrgIds(List.of(RESPONDENT1))
            .orgPolicyCaseAssignedRoles(List.of(CASE_ROLE))
            .build();

        return List.of(dateItem1, dateItem2);
    }
}
