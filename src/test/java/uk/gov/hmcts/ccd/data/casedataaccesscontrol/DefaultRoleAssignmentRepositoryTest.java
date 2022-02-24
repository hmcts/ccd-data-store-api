package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleType;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doReturn;
import static org.springframework.http.HttpMethod.POST;

public class DefaultRoleAssignmentRepositoryTest {

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ResponseEntity<RoleAssignmentResponse> responseEntity;

    @InjectMocks
    DefaultRoleAssignmentRepository defaultRoleAssignmentRepository;

    private static final String ID = "4d96923f-891a-4cb1-863e-9bec44d1689d";
    private static final String ACTOR_ID = "567567";
    private static final String ROLE_TYPE = RoleType.ORGANISATION.name();
    private static final String ROLE_NAME = "judge";

    private static final String ATTRIBUTES_CONTRACT_TYPE = "SALARIED";
    private static final String ATTRIBUTES_CASE_ID = "1504259907353529";

    private static final String AM_URI = "/am/role-assignments/query";

    private static final String TEST_PAGE_SIZE = "1";
    private static final String TEST_TOTAL_RECORDS = "2";

    private static final String HTTP_400_ERROR_MESSAGE = "Client error when getting Role Assignments from "
        + "Role Assignment Service because of 400 BAD_REQUEST";
    private static final String HTTP_404_ERROR_MESSAGE = "No Role Assignments found for userIds=[%s] and "
        + "casesIds=[%s] when getting from Role Assignment Service because of 404 NOT_FOUND";
    private static final String HTTP_500_ERROR_MESSAGE = "Problem getting Role Assignments from Role Assignment "
        + "Service because of 500 INTERNAL_SERVER_ERROR";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        defaultRoleAssignmentRepository = new DefaultRoleAssignmentRepository(
            applicationParams, securityUtils, restTemplate);

        when(applicationParams.getRoleAssignmentPageSize()).thenReturn(TEST_PAGE_SIZE);
        when(applicationParams.amQueryRoleAssignmentsURL()).thenReturn(AM_URI);

        when(securityUtils.authorizationHeaders()).thenReturn(new HttpHeaders());

        responseEntity = ResponseEntity.ok().headers(responseHeaders()).body(responseBody());
        doReturn(responseEntity).when(restTemplate).exchange(eq(AM_URI), eq(POST),
            any(), eq(RoleAssignmentResponse.class));
    }

    @DisplayName("should return roleAssignments by user and roles")
    @Test
    void shouldReturnRoleAssignmentsByUserAndRoles() {
        when(applicationParams.isRoleAssignmentPaginationEnabled()).thenReturn(false);

        RoleAssignmentResponse roleAssignmentResponse = defaultRoleAssignmentRepository
            .findRoleAssignmentsByCasesAndUsers(Collections.singletonList(ATTRIBUTES_CASE_ID),
                Collections.singletonList(ACTOR_ID));

        assertThat(roleAssignmentResponse.getRoleAssignments().size(), is(1));
    }

    @DisplayName("should error on 404 when post FindRoleAssignmentsByCasesAndUsers")
    @Test
    void shouldErrorOn404WhenPostFindRoleAssignmentsByCasesAndUsers() {
        when(applicationParams.isRoleAssignmentPaginationEnabled()).thenReturn(false);
        HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(exception).when(restTemplate).exchange(eq(AM_URI), eq(POST), any(), eq(RoleAssignmentResponse.class));

        final ResourceNotFoundException resourceNotFoundException = assertThrows(
            ResourceNotFoundException.class, () -> defaultRoleAssignmentRepository
                .findRoleAssignmentsByCasesAndUsers(Collections.singletonList(ATTRIBUTES_CASE_ID),
                    Collections.singletonList(ACTOR_ID)));

        assertEquals(resourceNotFoundException.getMessage(),
            String.format(HTTP_404_ERROR_MESSAGE, ACTOR_ID, ATTRIBUTES_CASE_ID));
    }

    @DisplayName("should error on 500 when post FindRoleAssignmentsByCasesAndUsers")
    @Test
    void shouldErrorOn500WhenPostFindRoleAssignmentsByCasesAndUsers() {
        when(applicationParams.isRoleAssignmentPaginationEnabled()).thenReturn(false);
        HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        doThrow(exception).when(restTemplate).exchange(eq(AM_URI), eq(POST), any(), eq(RoleAssignmentResponse.class));

        final ServiceException serviceException = assertThrows(
            ServiceException.class, () -> defaultRoleAssignmentRepository
                .findRoleAssignmentsByCasesAndUsers(Collections.singletonList(ATTRIBUTES_CASE_ID),
                    Collections.singletonList(ACTOR_ID)));

        assertEquals(serviceException.getMessage(), HTTP_500_ERROR_MESSAGE);
    }

    @DisplayName("should error on 400 when post FindRoleAssignmentsByCasesAndUsers")
    @Test
    void shouldErrorOn400WhenPostFindRoleAssignmentsByCasesAndUsers() {
        when(applicationParams.isRoleAssignmentPaginationEnabled()).thenReturn(false);
        HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.BAD_REQUEST);
        doThrow(exception).when(restTemplate).exchange(eq(AM_URI), eq(POST), any(), eq(RoleAssignmentResponse.class));

        final BadRequestException badRequestException = assertThrows(
            BadRequestException.class, () -> defaultRoleAssignmentRepository
                .findRoleAssignmentsByCasesAndUsers(Collections.singletonList(ATTRIBUTES_CASE_ID),
                    Collections.singletonList(ACTOR_ID)));

        assertEquals(badRequestException.getMessage(), HTTP_400_ERROR_MESSAGE);
    }

    @DisplayName("should return roleAssignments by user and roles with pagination")
    @Test
    void shouldReturnRoleAssignmentsByUserAndRolesWithPagination() {
        when(applicationParams.isRoleAssignmentPaginationEnabled()).thenReturn(true);

        RoleAssignmentResponse roleAssignmentResponse = defaultRoleAssignmentRepository
            .findRoleAssignmentsByCasesAndUsers(Collections.singletonList(ATTRIBUTES_CASE_ID),
                Collections.singletonList(ACTOR_ID));

        assertThat(roleAssignmentResponse.getRoleAssignments().size(), is(2));
    }

    @DisplayName("should error on 404 when post FindRoleAssignmentsByCasesAndUsers with pagination")
    @Test
    void shouldErrorOn404WhenPostFindRoleAssignmentsByCasesAndUsersWithPagination() {
        when(applicationParams.isRoleAssignmentPaginationEnabled()).thenReturn(true);
        HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(exception).when(restTemplate).exchange(eq(AM_URI), eq(POST), any(), eq(RoleAssignmentResponse.class));

        final ResourceNotFoundException resourceNotFoundException = assertThrows(
            ResourceNotFoundException.class, () -> defaultRoleAssignmentRepository
                .findRoleAssignmentsByCasesAndUsers(Collections.singletonList(ATTRIBUTES_CASE_ID),
                    Collections.singletonList(ACTOR_ID)));

        assertEquals(resourceNotFoundException.getMessage(),
            String.format(HTTP_404_ERROR_MESSAGE, ACTOR_ID, ATTRIBUTES_CASE_ID));
    }

    @DisplayName("should error on 500 when post FindRoleAssignmentsByCasesAndUsers with pagination")
    @Test
    void shouldErrorOn500WhenPostFindRoleAssignmentsByCasesAndUsersWithPagination() {
        when(applicationParams.isRoleAssignmentPaginationEnabled()).thenReturn(true);
        HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        doThrow(exception).when(restTemplate).exchange(eq(AM_URI), eq(POST), any(), eq(RoleAssignmentResponse.class));

        final ServiceException serviceException = assertThrows(
            ServiceException.class, () -> defaultRoleAssignmentRepository
                .findRoleAssignmentsByCasesAndUsers(Collections.singletonList(ATTRIBUTES_CASE_ID),
                    Collections.singletonList(ACTOR_ID)));

        assertEquals(serviceException.getMessage(), HTTP_500_ERROR_MESSAGE);
    }

    @DisplayName("should error on 400 when post FindRoleAssignmentsByCasesAndUsers with pagination")
    @Test
    void shouldErrorOn400WhenPostFindRoleAssignmentsByCasesAndUsersWithPagination() {
        when(applicationParams.isRoleAssignmentPaginationEnabled()).thenReturn(true);
        HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.BAD_REQUEST);
        doThrow(exception).when(restTemplate).exchange(eq(AM_URI), eq(POST), any(), eq(RoleAssignmentResponse.class));

        final BadRequestException badRequestException = assertThrows(
            BadRequestException.class, () -> defaultRoleAssignmentRepository
                .findRoleAssignmentsByCasesAndUsers(Collections.singletonList(ATTRIBUTES_CASE_ID),
                    Collections.singletonList(ACTOR_ID)));

        assertEquals(badRequestException.getMessage(), HTTP_400_ERROR_MESSAGE);
    }

    private static HttpHeaders responseHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(DefaultRoleAssignmentRepository.ROLE_ASSIGNMENT_TOTAL_RECORDS_HEADER, TEST_TOTAL_RECORDS);
        return headers;
    }

    private static RoleAssignmentResponse responseBody() {
        RoleAssignmentResource roleAssignmentResource = new RoleAssignmentResource();
        roleAssignmentResource.setId(ID);
        roleAssignmentResource.setActorId(ACTOR_ID);
        roleAssignmentResource.setRoleName(ROLE_NAME);
        roleAssignmentResource.setRoleType(ROLE_TYPE);

        RoleAssignmentAttributesResource attributes = new RoleAssignmentAttributesResource();
        attributes.setContractType(Optional.of(ATTRIBUTES_CONTRACT_TYPE));
        attributes.setCaseId(Optional.of(ATTRIBUTES_CASE_ID));

        roleAssignmentResource.setAttributes(attributes);

        return new RoleAssignmentResponse(Collections.singletonList(roleAssignmentResource));
    }
}
