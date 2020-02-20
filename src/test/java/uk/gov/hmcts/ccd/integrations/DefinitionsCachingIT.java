package uk.gov.hmcts.ccd.integrations;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.definition.*;
import uk.gov.hmcts.ccd.data.user.DefaultUserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureWireMock(port = 0)
@TestPropertySource(locations = "classpath:integration_tests.properties")
public class DefinitionsCachingIT {

    private static final String ID_1 = "case1";
    private static final String ID_2 = "case11";
    private static final String ID_3 = "case111";
    private static final String EVENT_ID = "event1";
    private static final int VERSION_1 = 33;
    private static final int VERSION_2 = 3311;
    private static final int VERSION_3 = 331111;

    private static final Jurisdiction JURISDICTION_1 = new Jurisdiction();
    private static final Jurisdiction JURISDICTION_2 = new Jurisdiction();
    private static final Jurisdiction JURISDICTION_3 = new Jurisdiction();

    @SpyBean
    private DefaultCaseDefinitionRepository caseDefinitionRepository;

    @Autowired
    CachedCaseDefinitionRepository cachedCaseDefinitionRepository;

    @Autowired
    ApplicationParams applicationParams;

    @SpyBean
    UIDefinitionRepository uiDefinitionRepository;

    @SpyBean
    private HttpUIDefinitionGateway httpUIDefinitionGateway;

    @Mock
    CaseType mockCaseType;

    @Mock
    WorkbasketInputDefinition workbasketInputDefinition;

    @Mock
    SearchResult searchResult;

    @Mock
    CaseTabCollection caseTabCollection;

    @Mock
    SearchInputDefinition searchInputDefinition;

    @SpyBean
    private DefaultUserRepository userRepository;

    @MockBean
    @Qualifier("restTemplate")
    private RestTemplate restTemplate;

    @MockBean
    private SecurityUtils securityUtils;

    List<WizardPage> wizardPageList = Collections.emptyList();

    List<Banner> bannersList = Collections.emptyList();

    @Before
    public void setup() {
        doReturn(aCaseTypeDefVersion(VERSION_1)).when(this.caseDefinitionRepository).getLatestVersionFromDefinitionStore(ID_1);
        doReturn(aCaseTypeDefVersion(VERSION_2)).when(this.caseDefinitionRepository).getLatestVersionFromDefinitionStore(ID_2);
        doReturn(aCaseTypeDefVersion(VERSION_3)).when(this.caseDefinitionRepository).getLatestVersionFromDefinitionStore(ID_3);
        doReturn(JURISDICTION_1).when(this.caseDefinitionRepository).getJurisdictionFromDefinitionStore("J1");
        doReturn(JURISDICTION_2).when(this.caseDefinitionRepository).getJurisdictionFromDefinitionStore("J2");
        doReturn(JURISDICTION_3).when(this.caseDefinitionRepository).getJurisdictionFromDefinitionStore("J3");
        doReturn(mockCaseType).when(this.caseDefinitionRepository).getCaseType(ID_1);
    }

    @Test
    public void testJurisdictionListsAreCached() {
        verify(caseDefinitionRepository, times(0)).getJurisdiction("J1");
        cachedCaseDefinitionRepository.getJurisdiction("J1");
        verify(caseDefinitionRepository, times(1)).getJurisdiction("J1");
        cachedCaseDefinitionRepository.getJurisdiction("J1");
        verify(caseDefinitionRepository, times(1)).getJurisdiction("J1");
        cachedCaseDefinitionRepository.getJurisdiction("J1");
        verify(caseDefinitionRepository, times(1)).getJurisdiction("J1");
    }

    @Test
    public void testTtlBasedEvictionOfJurisdictionLists() throws InterruptedException {
        Assert.assertEquals(3, applicationParams.getJurisdictionTTLSecs());

        verify(caseDefinitionRepository, times(0)).getJurisdictionFromDefinitionStore("J2");
        caseDefinitionRepository.getJurisdiction("J2");
        verify(caseDefinitionRepository, times(1)).getJurisdictionFromDefinitionStore("J2");

        caseDefinitionRepository.getJurisdiction("J2");
        caseDefinitionRepository.getJurisdiction("J2");
        caseDefinitionRepository.getJurisdiction("J2");
        verify(caseDefinitionRepository, times(1)).getJurisdictionFromDefinitionStore("J2");

        TimeUnit.SECONDS.sleep(1);
        caseDefinitionRepository.getJurisdiction("J2");
        caseDefinitionRepository.getJurisdiction("J2");
        caseDefinitionRepository.getJurisdiction("J2");
        verify(caseDefinitionRepository, times(1)).getJurisdictionFromDefinitionStore("J2");

        TimeUnit.SECONDS.sleep(1);
        caseDefinitionRepository.getJurisdiction("J2");
        caseDefinitionRepository.getJurisdiction("J2");
        caseDefinitionRepository.getJurisdiction("J2");
        verify(caseDefinitionRepository, times(1)).getJurisdictionFromDefinitionStore("J2");

        TimeUnit.SECONDS.sleep(1);
        verify(caseDefinitionRepository, times(1)).getJurisdictionFromDefinitionStore("J2");
        caseDefinitionRepository.getJurisdiction("J2");
        verify(caseDefinitionRepository, times(2)).getJurisdictionFromDefinitionStore("J2");

        caseDefinitionRepository.getJurisdiction("J2");
        caseDefinitionRepository.getJurisdiction("J2");
        caseDefinitionRepository.getJurisdiction("J2");
        verify(caseDefinitionRepository, times(2)).getJurisdictionFromDefinitionStore("J2");
    }

    @Test
    public void testCaseDefinitionLatestVersionsAreCached() {
        Assert.assertEquals(3, applicationParams.getLatestVersionTTLSecs());
        verify(caseDefinitionRepository, times(0)).getLatestVersion(ID_2);
        cachedCaseDefinitionRepository.getLatestVersion(ID_2);
        verify(caseDefinitionRepository, times(1)).getLatestVersion(ID_2);
        cachedCaseDefinitionRepository.getLatestVersion(ID_2);
        verify(caseDefinitionRepository, times(1)).getLatestVersion(ID_2);
        cachedCaseDefinitionRepository.getLatestVersion(ID_2);
        verify(caseDefinitionRepository, times(1)).getLatestVersion(ID_2);
    }

    @Test
    public void testTtlBasedEvictionOfCaseDefinitionLatestVersion() throws InterruptedException {
        Assert.assertEquals(3, applicationParams.getLatestVersionTTLSecs());

        verify(caseDefinitionRepository, times(0)).getLatestVersion(ID_3);
        caseDefinitionRepository.getLatestVersion(ID_3);
        verify(caseDefinitionRepository, times(1)).getLatestVersion(ID_3);

        caseDefinitionRepository.getLatestVersion(ID_3);
        caseDefinitionRepository.getLatestVersion(ID_3);
        caseDefinitionRepository.getLatestVersion(ID_3);
        verify(caseDefinitionRepository, times(1)).getLatestVersion(ID_3);

        TimeUnit.SECONDS.sleep(1);
        caseDefinitionRepository.getLatestVersion(ID_3);
        caseDefinitionRepository.getLatestVersion(ID_3);
        caseDefinitionRepository.getLatestVersion(ID_3);
        verify(caseDefinitionRepository, times(1)).getLatestVersion(ID_3);

        TimeUnit.SECONDS.sleep(4);
        verify(caseDefinitionRepository, times(1)).getLatestVersion(ID_3);
        caseDefinitionRepository.getLatestVersion(ID_3);
        verify(caseDefinitionRepository, times(2)).getLatestVersion(ID_3);

        caseDefinitionRepository.getLatestVersion(ID_3);
        caseDefinitionRepository.getLatestVersion(ID_3);
        caseDefinitionRepository.getLatestVersion(ID_3);
        verify(caseDefinitionRepository, times(2)).getLatestVersion(ID_3);
    }

    @Test
    public void testCaseDefinitionAreCached() {

        cachedCaseDefinitionRepository.getCaseType(ID_1);
        cachedCaseDefinitionRepository.getCaseType(ID_1);
        cachedCaseDefinitionRepository.getCaseType(ID_1);

        verify(caseDefinitionRepository, times(1)).getCaseType(VERSION_1, ID_1);
        verify(caseDefinitionRepository, times(1)).getCaseType(ID_1);
    }

    @Test
    public void testWorkbasketInputDefinitionsAreCached() {

        doReturn(workbasketInputDefinition).when(this.httpUIDefinitionGateway).getWorkbasketInputDefinitions(VERSION_1, ID_1);

        uiDefinitionRepository.getWorkbasketInputDefinitions(ID_1);
        uiDefinitionRepository.getWorkbasketInputDefinitions(ID_1);
        uiDefinitionRepository.getWorkbasketInputDefinitions(ID_1);

        verify(httpUIDefinitionGateway, times(1)).getWorkbasketInputDefinitions(VERSION_1, ID_1);
    }

    @Test
    public void testWorkbasketResultAreCached() {

        doReturn(searchResult).when(this.httpUIDefinitionGateway).getWorkBasketResult(VERSION_1, ID_1);

        uiDefinitionRepository.getWorkBasketResult(ID_1);
        uiDefinitionRepository.getWorkBasketResult(ID_1);
        uiDefinitionRepository.getWorkBasketResult(ID_1);

        verify(httpUIDefinitionGateway, times(1)).getWorkBasketResult(VERSION_1, ID_1);
    }

    @Test
    public void testSearchResultAreCached() {

        doReturn(searchResult).when(this.httpUIDefinitionGateway).getSearchResult(VERSION_1, ID_1);

        uiDefinitionRepository.getSearchResult(ID_1);
        uiDefinitionRepository.getSearchResult(ID_1);
        uiDefinitionRepository.getSearchResult(ID_1);

        verify(httpUIDefinitionGateway, times(1)).getSearchResult(VERSION_1, ID_1);
    }

    @Test
    public void testCaseTabsAreCached() {

        doReturn(caseTabCollection).when(this.httpUIDefinitionGateway).getCaseTabCollection(VERSION_1, ID_1);

        uiDefinitionRepository.getCaseTabCollection(ID_1);
        uiDefinitionRepository.getCaseTabCollection(ID_1);
        uiDefinitionRepository.getCaseTabCollection(ID_1);

        verify(httpUIDefinitionGateway, times(1)).getCaseTabCollection(VERSION_1, ID_1);
    }

    @Test
    public void testSearchInputDefinitionsAreCached() {

        doReturn(searchInputDefinition).when(this.httpUIDefinitionGateway).getSearchInputDefinitions(VERSION_1, ID_1);

        uiDefinitionRepository.getSearchInputDefinitions(ID_1);
        uiDefinitionRepository.getSearchInputDefinitions(ID_1);
        uiDefinitionRepository.getSearchInputDefinitions(ID_1);

        verify(httpUIDefinitionGateway, times(1)).getSearchInputDefinitions(VERSION_1, ID_1);
    }

    @Test
    public void testWizardPageDefinitionsAreCached() {

        doReturn(wizardPageList).when(this.httpUIDefinitionGateway).getWizardPageCollection(VERSION_1, ID_1, EVENT_ID);

        uiDefinitionRepository.getWizardPageCollection(ID_1, EVENT_ID);
        uiDefinitionRepository.getWizardPageCollection(ID_1, EVENT_ID);
        uiDefinitionRepository.getWizardPageCollection(ID_1, EVENT_ID);

        verify(httpUIDefinitionGateway, times(1)).getWizardPageCollection(VERSION_1, ID_1, EVENT_ID);
    }

    protected CaseTypeDefinitionVersion aCaseTypeDefVersion(int version) {
        CaseTypeDefinitionVersion ctdv = new CaseTypeDefinitionVersion();
        ctdv.setVersion(version);
        return ctdv;
    }


    @Test
    public void testBannersCached() {
        List<String> jurisdictionIds = new ArrayList<>();
        BannersResult bannersResult = new BannersResult(bannersList);
        doReturn(bannersResult).when(this.httpUIDefinitionGateway).getBanners(jurisdictionIds);

        uiDefinitionRepository.getBanners(jurisdictionIds);
        uiDefinitionRepository.getBanners(jurisdictionIds);
        uiDefinitionRepository.getBanners(jurisdictionIds);

        verify(httpUIDefinitionGateway, times(1)).getBanners(jurisdictionIds);
    }

}

