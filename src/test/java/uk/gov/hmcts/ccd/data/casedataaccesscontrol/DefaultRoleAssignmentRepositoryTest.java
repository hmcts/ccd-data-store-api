package uk.gov.hmcts.ccd.data.casedataaccesscontrol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleType;

import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
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

    @DisplayName("should return roleAssignments by user and roles with pagination")
    @Test
    void shouldReturnRoleAssignmentsByUserAndRolesWithPagination() {
        when(applicationParams.isRoleAssignmentPaginationEnabled()).thenReturn(true);

        RoleAssignmentResponse roleAssignmentResponse = defaultRoleAssignmentRepository
            .findRoleAssignmentsByCasesAndUsers(Collections.singletonList(ATTRIBUTES_CASE_ID),
                Collections.singletonList(ACTOR_ID));

        assertThat(roleAssignmentResponse.getRoleAssignments().size(), is(2));
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
