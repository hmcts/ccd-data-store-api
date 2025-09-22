package uk.gov.hmcts.ccd.integrations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import uk.gov.hmcts.ccd.data.caseaccess.DefaultCaseRoleRepository;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;

import static java.util.Collections.singletonList;

import uk.gov.hmcts.ccd.WireMockBaseTest;

@DirtiesContext
public class CaseRoleDefinitionCachingIT extends WireMockBaseTest {

    @MockitoSpyBean
    private DefaultCaseRoleRepository caseRoleRepository;

    private final String caseTypeId1 = "caseTypeId1";
    private final String caseTypeId2 = "caseTypeId2";

    @BeforeEach
    public void setup() {
        doReturn(new HashSet<>(singletonList("role1")))
            .when(this.caseRoleRepository)
            .getCaseRoles(DefaultCaseRoleRepository.DEFAULT_USER_ID,
                DefaultCaseRoleRepository.DEFAULT_JURISDICTION_ID,
                caseTypeId1);

        doReturn(new HashSet<>())
            .when(this.caseRoleRepository)
            .getCaseRoles(DefaultCaseRoleRepository.DEFAULT_USER_ID,
                DefaultCaseRoleRepository.DEFAULT_JURISDICTION_ID,
                caseTypeId2);
    }

    @Test
    @DisplayName("return case roles from cache and repository after cache eviction")
    public void returnCaseRolesForCaseTypeFromCacheAndRepositoryAfterCacheEviction() throws InterruptedException {

        assertEquals(3, applicationParams.getDefaultCacheTtlSecs());
        verify(caseRoleRepository, times(0)).getCaseRoles(caseTypeId1);

        caseRoleRepository.getCaseRoles(caseTypeId1);
        caseRoleRepository.getCaseRoles(caseTypeId1);
        caseRoleRepository.getCaseRoles(caseTypeId1);
        verify(caseRoleRepository, times(1)).getCaseRoles(caseTypeId1);

        caseRoleRepository.getCaseRoles(caseTypeId1);
        caseRoleRepository.getCaseRoles(caseTypeId1);
        verify(caseRoleRepository, times(1)).getCaseRoles(caseTypeId1);

        TimeUnit.SECONDS.sleep(3);

        caseRoleRepository.getCaseRoles(caseTypeId1);
        caseRoleRepository.getCaseRoles(caseTypeId1);
        caseRoleRepository.getCaseRoles(caseTypeId1);
        caseRoleRepository.getCaseRoles(caseTypeId1);
        verify(caseRoleRepository, times(2)).getCaseRoles(caseTypeId1);
    }

    @Test
    @DisplayName("return empty case roles but do not cache the result")
    public void returnEmptyCaseRolesForCaseTypeButDoNotCache() throws InterruptedException {

        assertEquals(3, applicationParams.getDefaultCacheTtlSecs());
        verify(caseRoleRepository, times(0)).getCaseRoles(caseTypeId2);

        caseRoleRepository.getCaseRoles(caseTypeId2);
        caseRoleRepository.getCaseRoles(caseTypeId2);
        caseRoleRepository.getCaseRoles(caseTypeId2);
        verify(caseRoleRepository, times(3)).getCaseRoles(caseTypeId2);

        TimeUnit.SECONDS.sleep(3);

        caseRoleRepository.getCaseRoles(caseTypeId2);
        caseRoleRepository.getCaseRoles(caseTypeId2);
        verify(caseRoleRepository, times(5)).getCaseRoles(caseTypeId2);
    }
}
