package uk.gov.hmcts.ccd.integrations;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CachedUIDefinitionGateway;
import uk.gov.hmcts.ccd.data.definition.CaseTypeDefinitionVersion;
import uk.gov.hmcts.ccd.data.definition.DefaultCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.HttpUIDefinitionGateway;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.Banner;
import uk.gov.hmcts.ccd.domain.model.definition.BannersResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeTabsDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchInputFieldsDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResultDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResultField;
import uk.gov.hmcts.ccd.domain.model.definition.SortOrder;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPageField;
import uk.gov.hmcts.ccd.domain.model.definition.WorkbasketInputFieldsDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureWireMock(port = 0)
@TestPropertySource(locations = "classpath:test.properties")
public class DefinitionsCachingIT {

    private static final String ID_1 = "case1";
    private static final String ID_2 = "case11";
    private static final String ID_3 = "case111";
    private static final String EVENT_ID = "event1";

    private static final int VERSION_1 = 33;
    private static final int VERSION_2 = 3311;
    private static final int VERSION_3 = 331111;

    private static final String USE_CASE_1 = "usecase1";

    private static final JurisdictionDefinition JURISDICTION_DEFINITION_1 = new JurisdictionDefinition();
    private static final JurisdictionDefinition JURISDICTION_DEFINITION_2 = new JurisdictionDefinition();
    private static final JurisdictionDefinition JURISDICTION_DEFINITION_3 = new JurisdictionDefinition();

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

    @SpyBean
    private CachedUIDefinitionGateway cachedUIDefinitionGateway;

    @Mock
    CaseTypeDefinition mockCaseTypeDefinition;

    @Mock
    WorkbasketInputFieldsDefinition workbasketInputFieldsDefinition;

    @Mock
    SearchResultDefinition searchResult;

    @Mock
    CaseTypeTabsDefinition caseTypeTabsDefinition;

    @Mock
    SearchInputFieldsDefinition searchInputFieldsDefinition;

    List<WizardPage> wizardPageList = new ArrayList<>();
    List<Banner> bannersList = Collections.emptyList();

    @Before
    public void setup() {
        initiateWizardPageList(wizardPageList);

        doReturn(caseTypeDefinitionVersion(VERSION_1)).when(this.caseDefinitionRepository)
            .getLatestVersionFromDefinitionStore(ID_1);
        doReturn(caseTypeDefinitionVersion(VERSION_2)).when(this.caseDefinitionRepository)
            .getLatestVersionFromDefinitionStore(ID_2);
        doReturn(caseTypeDefinitionVersion(VERSION_3)).when(this.caseDefinitionRepository)
            .getLatestVersionFromDefinitionStore(ID_3);
        doReturn(JURISDICTION_DEFINITION_1).when(this.caseDefinitionRepository)
            .getJurisdictionFromDefinitionStore("J1");
        doReturn(JURISDICTION_DEFINITION_2).when(this.caseDefinitionRepository)
            .getJurisdictionFromDefinitionStore("J2");
        doReturn(JURISDICTION_DEFINITION_3).when(this.caseDefinitionRepository)
            .getJurisdictionFromDefinitionStore("J3");
        doReturn(mockCaseTypeDefinition).when(this.caseDefinitionRepository).getCaseType(ID_1);

        doReturn(wizardPageList).when(this.httpUIDefinitionGateway).getWizardPageCollection(VERSION_1, ID_1, EVENT_ID);
        SearchResultDefinition searchResultDefinition = createSearchResultDefinition();
        doReturn(searchResultDefinition).when(this.httpUIDefinitionGateway).getWorkBasketResult(VERSION_1, ID_1);
        doReturn(searchResultDefinition).when(this.httpUIDefinitionGateway).getSearchResult(VERSION_1, ID_1);
        doReturn(searchResultDefinition).when(this.httpUIDefinitionGateway)
            .getSearchCasesResultDefinition(VERSION_1, ID_1, USE_CASE_1);
    }

    private void initiateWizardPageList(List<WizardPage> wizardPageList) {

        WizardPageField wizardPageField = new WizardPageField();
        wizardPageField.setCaseFieldId("case_field_1");
        List<WizardPageField> wizardPageFields = new ArrayList<>();
        wizardPageFields.add(wizardPageField);
        WizardPage wizardPage = new WizardPage();
        wizardPage.setWizardPageFields(wizardPageFields);
        wizardPageList.add(wizardPage);
    }

    private SearchResultDefinition createSearchResultDefinition() {
        SortOrder sortOrder = new SortOrder();
        sortOrder.setDirection("direction");
        sortOrder.setPriority(1);

        SearchResultField searchResultField = new SearchResultField();
        searchResultField.setCaseFieldId("casefield_1");
        searchResultField.setSortOrder(sortOrder);

        SearchResultField[] searchResultFields = new SearchResultField[]{searchResultField};
        SearchResultDefinition searchResultDefinition = new SearchResultDefinition();
        searchResultDefinition.setFields(searchResultFields);
        return searchResultDefinition;
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

        TimeUnit.SECONDS.sleep(2);
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
        Assert.assertEquals(3, applicationParams.getDefaultCacheTtlSecs());
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
        Assert.assertEquals(3, applicationParams.getDefaultCacheTtlSecs());

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

        TimeUnit.SECONDS.sleep(2);
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

        doReturn(workbasketInputFieldsDefinition).when(this.httpUIDefinitionGateway)
            .getWorkbasketInputFieldsDefinitions(VERSION_1, ID_1);

        uiDefinitionRepository.getWorkbasketInputDefinitions(ID_1);
        uiDefinitionRepository.getWorkbasketInputDefinitions(ID_1);
        uiDefinitionRepository.getWorkbasketInputDefinitions(ID_1);

        verify(httpUIDefinitionGateway, times(1)).getWorkbasketInputFieldsDefinitions(VERSION_1, ID_1);
    }

    @Test
    public void testWorkbasketResultAreCached() {
        uiDefinitionRepository.getWorkBasketResult(ID_1);
        uiDefinitionRepository.getWorkBasketResult(ID_1);
        uiDefinitionRepository.getWorkBasketResult(ID_1);

        verify(httpUIDefinitionGateway, times(1)).getWorkBasketResult(VERSION_1, ID_1);
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void testWorkbasketResultPreventCacheManipulation() {
        var searchResultDefinitionFirstAttempt = uiDefinitionRepository.getWorkBasketResult(ID_1);
        //change cached data
        searchResultDefinitionFirstAttempt.setFields(null);
        Assert.assertNull(searchResultDefinitionFirstAttempt.getFields());

        var searchResultDefinitionSecondAttempt = uiDefinitionRepository.getWorkBasketResult(ID_1);

        assertEquals(1, searchResultDefinitionSecondAttempt.getFields().length);

        searchResultDefinitionSecondAttempt.setFields(null);
        assertNull(searchResultDefinitionSecondAttempt.getFields());

        var searchResultDefinitionThirdAttempt = uiDefinitionRepository.getWorkBasketResult(ID_1);

        assertEquals(1, searchResultDefinitionThirdAttempt.getFields().length);

        verify(httpUIDefinitionGateway, times(1)).getWorkBasketResult(VERSION_1, ID_1);
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void testWorkbasketResultFailWithCacheManipulationWithoutDeepCopy() {
        var searchResultDefinitionFirstAttempt = cachedUIDefinitionGateway.getWorkBasketResult(VERSION_1, ID_1);
        //change cached data
        searchResultDefinitionFirstAttempt.setFields(null);
        Assert.assertNull(searchResultDefinitionFirstAttempt.getFields());

        var searchResultDefinitionSecondAttempt = cachedUIDefinitionGateway.getWorkBasketResult(VERSION_1, ID_1);
        Assert.assertNull(searchResultDefinitionSecondAttempt.getFields());

        var searchResultDefinitionThirdAttempt = cachedUIDefinitionGateway.getWorkBasketResult(VERSION_1, ID_1);
        Assert.assertNull(searchResultDefinitionThirdAttempt.getFields());

        verify(httpUIDefinitionGateway, times(1)).getWorkBasketResult(VERSION_1, ID_1);
    }

    @Test
    public void testSearchResultAreCached() {
        uiDefinitionRepository.getSearchResult(ID_1);
        uiDefinitionRepository.getSearchResult(ID_1);
        uiDefinitionRepository.getSearchResult(ID_1);

        verify(httpUIDefinitionGateway, times(1)).getSearchResult(VERSION_1, ID_1);
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void testSearchResultPreventCacheManipulation() {
        var searchResultDefinitionFirstAttempt = uiDefinitionRepository.getSearchResult(ID_1);
        //change cached data
        searchResultDefinitionFirstAttempt.setFields(null);
        Assert.assertNull(searchResultDefinitionFirstAttempt.getFields());

        var searchResultDefinitionSecondAttempt = uiDefinitionRepository.getSearchResult(ID_1);

        assertEquals(1, searchResultDefinitionSecondAttempt.getFields().length);

        searchResultDefinitionSecondAttempt.setFields(null);
        assertNull(searchResultDefinitionSecondAttempt.getFields());

        var searchResultDefinitionThirdAttempt = uiDefinitionRepository.getSearchResult(ID_1);

        assertEquals(1, searchResultDefinitionThirdAttempt.getFields().length);

        verify(httpUIDefinitionGateway, times(1)).getSearchResult(VERSION_1, ID_1);
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void testSearchResultFailWithCacheManipulationWithoutDeepCopy() {
        var searchResultDefinitionFirstAttempt = cachedUIDefinitionGateway.getSearchResult(VERSION_1, ID_1);
        //change cached data
        searchResultDefinitionFirstAttempt.setFields(null);
        Assert.assertNull(searchResultDefinitionFirstAttempt.getFields());

        var searchResultDefinitionSecondAttempt = cachedUIDefinitionGateway.getSearchResult(VERSION_1, ID_1);
        Assert.assertNull(searchResultDefinitionSecondAttempt.getFields());

        var searchResultDefinitionThirdAttempt = cachedUIDefinitionGateway.getSearchResult(VERSION_1, ID_1);
        Assert.assertNull(searchResultDefinitionThirdAttempt.getFields());

        verify(httpUIDefinitionGateway, times(1)).getSearchResult(VERSION_1, ID_1);
    }

    @Test
    public void testSearchCasesResultAreCached() {
        uiDefinitionRepository.getSearchCasesResult(ID_1, USE_CASE_1);
        uiDefinitionRepository.getSearchCasesResult(ID_1, USE_CASE_1);
        uiDefinitionRepository.getSearchCasesResult(ID_1, USE_CASE_1);

        verify(httpUIDefinitionGateway, times(1))
            .getSearchCasesResultDefinition(VERSION_1, ID_1, USE_CASE_1);
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void testSearchCasesResultPreventCacheManipulation() {
        var searchResultDefinitionFirstAttempt = uiDefinitionRepository.getSearchCasesResult(ID_1, USE_CASE_1);
        //change cached data
        searchResultDefinitionFirstAttempt.setFields(null);
        Assert.assertNull(searchResultDefinitionFirstAttempt.getFields());

        var searchResultDefinitionSecondAttempt = uiDefinitionRepository.getSearchCasesResult(ID_1, USE_CASE_1);

        assertEquals(1, searchResultDefinitionSecondAttempt.getFields().length);

        searchResultDefinitionSecondAttempt.setFields(null);
        assertNull(searchResultDefinitionSecondAttempt.getFields());

        var searchResultDefinitionThirdAttempt = uiDefinitionRepository.getSearchCasesResult(ID_1, USE_CASE_1);

        assertEquals(1, searchResultDefinitionThirdAttempt.getFields().length);

        verify(httpUIDefinitionGateway, times(1))
            .getSearchCasesResultDefinition(VERSION_1, ID_1, USE_CASE_1);
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void testSearchCasesResultFailWithCacheManipulationWithoutDeepCopy() {
        var searchResultDefinitionFirstAttempt = cachedUIDefinitionGateway
            .getSearchCasesResultDefinition(VERSION_1, ID_1, USE_CASE_1);
        //change cached data
        searchResultDefinitionFirstAttempt.setFields(null);
        Assert.assertNull(searchResultDefinitionFirstAttempt.getFields());

        var searchResultDefinitionSecondAttempt = cachedUIDefinitionGateway
            .getSearchCasesResultDefinition(VERSION_1, ID_1, USE_CASE_1);
        Assert.assertNull(searchResultDefinitionSecondAttempt.getFields());

        var searchResultDefinitionThirdAttempt = cachedUIDefinitionGateway
            .getSearchCasesResultDefinition(VERSION_1, ID_1, USE_CASE_1);
        Assert.assertNull(searchResultDefinitionThirdAttempt.getFields());

        verify(httpUIDefinitionGateway, times(1))
            .getSearchCasesResultDefinition(VERSION_1, ID_1, USE_CASE_1);
    }

    @Test
    public void testCaseTabsAreCached() {

        doReturn(caseTypeTabsDefinition).when(this.httpUIDefinitionGateway).getCaseTypeTabsCollection(VERSION_1, ID_1);

        uiDefinitionRepository.getCaseTabCollection(ID_1);
        uiDefinitionRepository.getCaseTabCollection(ID_1);
        uiDefinitionRepository.getCaseTabCollection(ID_1);

        verify(httpUIDefinitionGateway, times(1)).getCaseTypeTabsCollection(VERSION_1, ID_1);
    }

    @Test
    public void testSearchInputDefinitionsAreCached() {

        doReturn(searchInputFieldsDefinition).when(this.httpUIDefinitionGateway)
            .getSearchInputFieldDefinitions(VERSION_1, ID_1);

        uiDefinitionRepository.getSearchInputFieldDefinitions(ID_1);
        uiDefinitionRepository.getSearchInputFieldDefinitions(ID_1);
        uiDefinitionRepository.getSearchInputFieldDefinitions(ID_1);

        verify(httpUIDefinitionGateway, times(1))
            .getSearchInputFieldDefinitions(VERSION_1, ID_1);
    }

    @Test
    public void testWizardPageDefinitionsAreCached() {

        doReturn(wizardPageList).when(this.httpUIDefinitionGateway).getWizardPageCollection(VERSION_1, ID_1, EVENT_ID);

        uiDefinitionRepository.getWizardPageCollection(ID_1, EVENT_ID);
        uiDefinitionRepository.getWizardPageCollection(ID_1, EVENT_ID);
        uiDefinitionRepository.getWizardPageCollection(ID_1, EVENT_ID);

        verify(httpUIDefinitionGateway, times(1)).getWizardPageCollection(VERSION_1, ID_1, EVENT_ID);
    }

    @Test
    public void testWizardPageDefinitionsPreventCacheManipulation() {
        var wizardPagesFirstAttempt = uiDefinitionRepository.getWizardPageCollection(ID_1, EVENT_ID);
        var wizardPageFirstAttempt = wizardPagesFirstAttempt.get(0);
        //change cached data
        wizardPagesFirstAttempt.forEach(wizardPage -> wizardPage.setWizardPageFields(null));
        Assert.assertNull(wizardPageFirstAttempt.getWizardPageFields());

        var wizardPagesSecondAttempt = uiDefinitionRepository.getWizardPageCollection(ID_1, EVENT_ID);
        var wizardPageSecondAttempt = wizardPagesSecondAttempt.get(0);

        Assert.assertFalse(wizardPageSecondAttempt.getWizardPageFields().isEmpty());

        wizardPagesSecondAttempt.forEach(wizardPage -> wizardPage.setWizardPageFields(null));
        Assert.assertNull(wizardPageSecondAttempt.getWizardPageFields());

        var wizardPagesThirdAttempt = uiDefinitionRepository.getWizardPageCollection(ID_1, EVENT_ID);
        var wizardPageThirdAttempt = wizardPagesThirdAttempt.get(0);

        Assert.assertFalse(wizardPageThirdAttempt.getWizardPageFields().isEmpty());

        verify(httpUIDefinitionGateway, times(1)).getWizardPageCollection(VERSION_1, ID_1, EVENT_ID);
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void testWizardPageDefinitionsFailWithCacheManipulationWithoutDeepCopy() {
        var wizardPagesFirstAttempt = cachedUIDefinitionGateway.getWizardPageCollection(VERSION_1, ID_1, EVENT_ID);
        var wizardPageFirstAttempt = wizardPagesFirstAttempt.get(0);
        //change cached data
        wizardPagesFirstAttempt.forEach(wizardPage -> wizardPage.setWizardPageFields(null));
        Assert.assertNull(wizardPageFirstAttempt.getWizardPageFields());

        var wizardPagesSecondAttempt = cachedUIDefinitionGateway.getWizardPageCollection(VERSION_1, ID_1, EVENT_ID);
        var wizardPageSecondAttempt = wizardPagesSecondAttempt.get(0);

        Assert.assertNull(wizardPageSecondAttempt.getWizardPageFields());

        var wizardPagesThirdAttempt = cachedUIDefinitionGateway.getWizardPageCollection(VERSION_1, ID_1, EVENT_ID);
        var wizardPageThirdAttempt = wizardPagesThirdAttempt.get(0);

        Assert.assertNull(wizardPageThirdAttempt.getWizardPageFields());

        verify(httpUIDefinitionGateway, times(1)).getWizardPageCollection(VERSION_1, ID_1, EVENT_ID);
    }

    protected CaseTypeDefinitionVersion caseTypeDefinitionVersion(int version) {
        CaseTypeDefinitionVersion ctdv = new CaseTypeDefinitionVersion();
        ctdv.setVersion(version);
        return ctdv;
    }

    @Test
    public void testBannersCached() {
        List<String> jurisdictionIds = new ArrayList<>();
        jurisdictionIds.add("123");
        BannersResult bannersResult = new BannersResult(bannersList);
        doReturn(bannersResult).when(this.httpUIDefinitionGateway).getBanners(jurisdictionIds);

        uiDefinitionRepository.getBanners(jurisdictionIds);
        jurisdictionIds = new ArrayList<>();
        jurisdictionIds.add("123");
        uiDefinitionRepository.getBanners(jurisdictionIds);
        uiDefinitionRepository.getBanners(jurisdictionIds);

        verify(httpUIDefinitionGateway, times(1)).getBanners(jurisdictionIds);
    }

}

