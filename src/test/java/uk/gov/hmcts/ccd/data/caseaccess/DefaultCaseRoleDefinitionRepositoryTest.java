package uk.gov.hmcts.ccd.data.caseaccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseRoleDefinition;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.GET;

class DefaultCaseRoleDefinitionRepositoryTest {
    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private ResponseEntity<CaseRoleDefinition[]> responseEntity;

    private DefaultCaseRoleRepository caseRoleRepository;

    private final String caseTypeId = "caseTypeId";

    private final CaseRoleDefinition caseRoleDefinition1 = new CaseRoleDefinition();
    private final CaseRoleDefinition caseRoleDefinition2 = new CaseRoleDefinition();
    private final CaseRoleDefinition[] caseRoleDefinitions = {caseRoleDefinition1, caseRoleDefinition2};
    private static final String NAME1 = "name1";
    private static final String NAME2 = "name2";
    private static final String ROLE1 = "role1";
    private static final String ROLE2 = "role2";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        caseRoleRepository = new DefaultCaseRoleRepository(applicationParams, securityUtils, restTemplate);
        caseRoleDefinition1.setId(ROLE1);
        caseRoleDefinition1.setName(NAME1);
        caseRoleDefinition2.setId(ROLE2);
        caseRoleDefinition2.setName(NAME2);

        when(securityUtils.userAuthorizationHeaders()).thenReturn(new HttpHeaders());
        String caseRolesUrl = "/api/data/caseworkers/uid/jurisdictions/jid/case-types/caseTypeId/roles";
        when(applicationParams.caseRolesURL(
            DefaultCaseRoleRepository.DEFAULT_USER_ID, DefaultCaseRoleRepository.DEFAULT_JURISDICTION_ID, caseTypeId))
            .thenReturn(caseRolesUrl);
        doReturn(responseEntity).when(restTemplate).exchange(eq(caseRolesUrl), eq(GET), any(),
            eq(CaseRoleDefinition[].class));
    }

    @Test
    @DisplayName("return case roles when case type has case roles")
    void returnCaseRolesByCaseTypeId() {
        when(responseEntity.getBody()).thenReturn(caseRoleDefinitions);

        Set<String> caseRoleSet = caseRoleRepository.getCaseRoles(caseTypeId);
        assertThat(caseRoleSet.size(), is(2));
    }

    @Test
    @DisplayName("return NO case roles when case type has NO case roles")
    void returnNoCaseRolesByCaseTypeIdWhenCaseTypeDoesNotHaveAny() {
        when(responseEntity.getBody()).thenReturn(new CaseRoleDefinition[0]);

        Set<String> caseRoleSet = caseRoleRepository.getCaseRoles(caseTypeId);
        assertThat(caseRoleSet.size(), is(0));
    }

    @Test
    @DisplayName("return NO case roles when case type has NO case roles")
    void returnNoCaseRolesByCaseTypeIdWhenCaseTypeReturnsNull() {
        when(responseEntity.getBody()).thenReturn(null);

        Set<String> caseRoleSet = caseRoleRepository.getCaseRoles(caseTypeId);
        assertThat(caseRoleSet.size(), is(0));
    }

    @Test
    @DisplayName("return case roles when case type has case roles")
    void returnCaseRolesForCaseType() {
        when(responseEntity.getBody()).thenReturn(caseRoleDefinitions);

        Set<String> caseRoleSet = caseRoleRepository.getCaseRoles(DefaultCaseRoleRepository.DEFAULT_USER_ID,
            DefaultCaseRoleRepository.DEFAULT_JURISDICTION_ID, caseTypeId);
        assertThat(caseRoleSet.size(), is(2));
    }

    @Test
    @DisplayName("return NO case roles when case type has NO case roles")
    void returnNoCaseRolesWhenCaseTypeDoesNotHaveAny() {
        when(responseEntity.getBody()).thenReturn(new CaseRoleDefinition[0]);

        Set<String> caseRoleSet = caseRoleRepository.getCaseRoles(DefaultCaseRoleRepository.DEFAULT_USER_ID,
            DefaultCaseRoleRepository.DEFAULT_JURISDICTION_ID, caseTypeId);
        assertThat(caseRoleSet.size(), is(0));
    }

    @Test
    @DisplayName("return null when case type has NO case roles")
    void returnNullWhenCaseTypeDoesNotHaveAny() {
        when(responseEntity.getBody()).thenReturn(null);

        Set<String> caseRoleSet = caseRoleRepository.getCaseRoles(DefaultCaseRoleRepository.DEFAULT_USER_ID,
            DefaultCaseRoleRepository.DEFAULT_JURISDICTION_ID, caseTypeId);
        assertThat(caseRoleSet.size(), is(0));
    }

    @Test
    @DisplayName("return case roles when case type has case roles and role assignment is true")
    void returnCaseRolesForCaseTypeAndRoleAssignment() {

        when(securityUtils.authorizationHeaders()).thenReturn(new HttpHeaders());
        when(applicationParams.accessProfileRolesURL(caseTypeId)).thenReturn("someURL");
        when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);
        doReturn(responseEntity).when(restTemplate).exchange(
            eq("someURL"), eq(GET), any(),
            eq(CaseRoleDefinition[].class)
        );
        when(responseEntity.getBody()).thenReturn(caseRoleDefinitions);
        Set<String> caseRoleSet = caseRoleRepository.getRoles("caseTypeId");

        assertThat(caseRoleSet.size(), is(2));
        assertThat(caseRoleSet.toArray()[0], is(NAME2));
        assertThat(caseRoleSet.toArray()[1], is(NAME1));
    }

    @Test
    @DisplayName("return case roles when case type has case roles and role assignment is false")
    void returnCaseRolesForCaseTypeAndRoleAssignmentIsFalse() {

        when(securityUtils.authorizationHeaders()).thenReturn(new HttpHeaders());
        when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(false);
        when(responseEntity.getBody()).thenReturn(caseRoleDefinitions);
        Set<String> caseRoleSet = caseRoleRepository.getRoles(caseTypeId);

        assertThat(caseRoleSet.size(), is(2));
        assertThat(caseRoleSet.toArray()[0], is(ROLE1));
        assertThat(caseRoleSet.toArray()[1], is(ROLE2));
    }

    @Test
    @DisplayName("return case roles when case type has case roles and role assignment is true")
    void returnCaseRolesForCaseTypeAndRoleAssignmentIsTrueAndThereAreNotRoles() {

        when(securityUtils.authorizationHeaders()).thenReturn(new HttpHeaders());
        when(applicationParams.accessProfileRolesURL(caseTypeId)).thenReturn("someURL");
        when(applicationParams.getEnableAttributeBasedAccessControl()).thenReturn(true);

        doReturn(responseEntity).when(restTemplate).exchange(
            eq("someURL"),
            eq(GET),
            any(),
            eq(CaseRoleDefinition[].class)
        );

        when(responseEntity.getBody()).thenReturn(new CaseRoleDefinition[]{});
        Set<String> caseRoleSet = caseRoleRepository.getRoles(caseTypeId);

        verify(restTemplate, times(2)).exchange(
            anyString(), eq(GET), any(), eq(CaseRoleDefinition[].class)
        );
        verify(responseEntity, times(2)).getBody();

        assertThat(caseRoleSet.size(), is(0));
    }
}
