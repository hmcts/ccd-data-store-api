package uk.gov.hmcts.ccd.v2.external.controller;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;
import uk.gov.hmcts.ccd.domain.service.cauroles.CaseAssignedUserRolesOperation;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.resource.CaseAssignedUserRolesResource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class CaseAssignedUserRolesControllerTest {

    @Mock
    private UIDService caseReferenceService;

    @Mock
    private CaseAssignedUserRolesOperation caseAssignedUserRolesOperation;

    private CaseAssignedUserRolesController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(caseReferenceService.validateUID(anyString())).thenCallRealMethod();
        when(caseAssignedUserRolesOperation.findCaseUserRoles(anyList(), anyList())).thenReturn(createCaseAssignedUserRoles());

        controller = new CaseAssignedUserRolesController(caseReferenceService, caseAssignedUserRolesOperation);
    }

    private List<CaseAssignedUserRole> createCaseAssignedUserRoles() {
        List<CaseAssignedUserRole> userRoles = Lists.newArrayList();
        userRoles.add(new CaseAssignedUserRole());
        userRoles.add(new CaseAssignedUserRole());
        return userRoles;
    }

    @Test
    void throwsExceptionWhenNullCaseIdListPassed() {
        BadRequestException exception = assertThrows(BadRequestException.class,
            () -> controller.getCaseUserRoles(null, Optional.of(Lists.newArrayList())));

        assertAll(
            () -> assertThat(exception.getMessage(),
                containsString(V2.Error.EMPTY_CASE_ID_LIST))
        );
    }

    @Test
    void throwsExceptionWhenEmptyCaseIdListPassed() {
        BadRequestException exception = assertThrows(BadRequestException.class,
            () -> controller.getCaseUserRoles(Lists.newArrayList(),
                Optional.of(Lists.newArrayList())));

        assertAll(
            () -> assertThat(exception.getMessage(),
                containsString(V2.Error.EMPTY_CASE_ID_LIST))
        );
    }

    @Test
    void throwsExceptionWhenEmptyCaseIdListContainsInvalidCaseId() {
        BadRequestException exception = assertThrows(BadRequestException.class,
            () -> controller.getCaseUserRoles(Lists.newArrayList("123456"),
                Optional.of(Lists.newArrayList())));

        assertAll(
            () -> assertThat(exception.getMessage(),
                containsString(V2.Error.CASE_ID_INVALID))
        );
    }

    @Test
    void throwsExceptionWhenInvalidUserIdListPassed() {
        BadRequestException exception = assertThrows(BadRequestException.class,
            () -> controller.getCaseUserRoles(Lists.newArrayList("6375837333991692"),
                Optional.of(Lists.newArrayList("8900", "", "89002"))));

        assertAll(
            () -> assertThat(exception.getMessage(),
                containsString(V2.Error.USER_ID_INVALID))
        );
    }

    @Test
    void shouldGetResponseWhenCaseIdsAndUserIdsPassed() {
        when(caseReferenceService.validateUID(anyString())).thenReturn(true);
        ResponseEntity<CaseAssignedUserRolesResource> response = controller.getCaseUserRoles(
            Lists.newArrayList("7578590391163133"),
            Optional.of(Lists.newArrayList("8900", "89002")));
        assertNotNull(response);
        assertEquals(2, response.getBody().getCaseAssignedUserRoles().size());
    }

    @Test
    void shouldGetResponseWhenCaseIdsPassed() {
        when(caseReferenceService.validateUID(anyString())).thenReturn(true);
        ResponseEntity<CaseAssignedUserRolesResource> response = controller.getCaseUserRoles(
            Lists.newArrayList("7578590391163133"),
            Optional.empty());
        assertNotNull(response);
        assertEquals(2, response.getBody().getCaseAssignedUserRoles().size());
    }
}
