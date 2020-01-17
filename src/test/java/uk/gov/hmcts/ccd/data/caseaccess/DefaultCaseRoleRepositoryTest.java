package uk.gov.hmcts.ccd.data.caseaccess;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.GET;

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
import uk.gov.hmcts.ccd.domain.model.definition.CaseRole;

class DefaultCaseRoleRepositoryTest {
    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private HttpHeaders httpHeaders;

    @Mock
    private ResponseEntity<CaseRole[]> responseEntity;

    private DefaultCaseRoleRepository caseRoleRepository;

    private CaseRole caseRole1 = new CaseRole();
    private CaseRole caseRole2 = new  CaseRole();
    private CaseRole[] caseRoles = {caseRole1, caseRole2};

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        caseRoleRepository = new DefaultCaseRoleRepository(applicationParams, securityUtils, restTemplate);
        caseRole1.setId("role1");
        caseRole2.setId("role2");
    }

    @Test
    @DisplayName("return case roles when case type has case roles")
    void returnCaseRolesForCaseType() {

        when(securityUtils.authorizationHeaders()).thenReturn(httpHeaders);
        when(applicationParams.caseRolesURL()).thenReturn("someURL");
        doReturn(responseEntity).when(restTemplate).exchange(eq("someURL/caseTypeId/roles"), eq(GET), any(), eq(CaseRole[].class));
        when(responseEntity.getBody()).thenReturn(caseRoles);
        Set<String> caseRoleSet = caseRoleRepository.getCaseRoles("caseTypeId");

        assertThat(caseRoleSet.size(), is(2));
    }

    @Test
    @DisplayName("return NO case roles when case type has NO case roles")
    void returnNoCaseRolesWhenCaseTypeDoesntHaveAny() {

        when(securityUtils.userAuthorizationHeaders()).thenReturn(httpHeaders);
        when(applicationParams.caseRolesURL()).thenReturn("someURL");
        doReturn(responseEntity).when(restTemplate).exchange(eq("someURL/caseTypeId/roles"), eq(GET), any(), eq(CaseRole[].class));
        when(responseEntity.getBody()).thenReturn(new CaseRole[0]);
        Set<String> caseRoleSet = caseRoleRepository.getCaseRoles("caseTypeId");

        assertThat(caseRoleSet.size(), is(0));
    }
}
