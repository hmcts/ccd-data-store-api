package uk.gov.hmcts.ccd.data.caseaccess;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureWireMock(port = 0)
@TestPropertySource(locations = "classpath:test.properties")
public class CaseRoleDefinitionRepositoryIT {

    @SpyBean
    private DefaultCaseRoleRepository caseRoleRepository;

    @Autowired
    private CachedCaseRoleRepository cachedCaseRoleRepository;

    private final String caseType1 = "CASETYPE1";
    private final Set<String> caseRoles = Sets.newHashSet("cr1", "cr2", "cr3");

    @Before
    public void setUp() {
        doReturn(caseRoles).when(caseRoleRepository).getCaseRoles(caseType1);
    }

    @Test
    public void shouldGetCaseRolesFromCache() {
        Set<String> returned = cachedCaseRoleRepository.getCaseRoles(caseType1);

        assertAll(
            () -> assertThat(returned, is(caseRoles)),
            () -> verify(caseRoleRepository, times(1)).getCaseRoles(caseType1)
        );

        Set<String> returned2 = cachedCaseRoleRepository.getCaseRoles(caseType1);

        assertAll(
            () -> assertThat(returned2, is(caseRoles)),
            () -> verifyNoMoreInteractions(caseRoleRepository)
        );
    }

}
