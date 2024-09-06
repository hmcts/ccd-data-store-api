package uk.gov.hmcts.ccd.integrations;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Disabled;
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
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPageField;
import uk.gov.hmcts.ccd.domain.model.definition.WorkbasketInputFieldsDefinition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    private static final String JURISDICTION_ID_1 = "J1";
    private static final String JURISDICTION_ID_2 = "J2";
    private static final String JURISDICTION_ID_3 = "J3";
    private static final String JURISDICTION_ID_4 = "J4";

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
        doReturn(List.of(JURISDICTION_DEFINITION_1)).when(this.caseDefinitionRepository)
            .retrieveJurisdictions(Optional.of(List.of(JURISDICTION_ID_1)));
        doReturn(List.of(JURISDICTION_DEFINITION_2)).when(this.caseDefinitionRepository)
            .retrieveJurisdictions(Optional.of(List.of(JURISDICTION_ID_2)));
        doReturn(List.of(JURISDICTION_DEFINITION_3)).when(this.caseDefinitionRepository)
            .retrieveJurisdictions(Optional.of(List.of(JURISDICTION_ID_3)));

        doReturn(mockCaseTypeDefinition).when(this.caseDefinitionRepository).getCaseType(ID_1);

        doReturn(wizardPageList).when(this.httpUIDefinitionGateway).getWizardPageCollection(VERSION_1, ID_1, EVENT_ID);
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

    private JurisdictionDefinition createJurisdictionDefinition(String jurisdictionId) {
        JurisdictionDefinition jurisdictionDefinition = new JurisdictionDefinition();
        jurisdictionDefinition.setId(jurisdictionId);
        CaseTypeDefinition caseType1 = new CaseTypeDefinition();
        caseType1.setId(ID_1);
        CaseTypeDefinition caseType2 = new CaseTypeDefinition();
        caseType1.setId(ID_2);
        jurisdictionDefinition.setCaseTypeDefinitions(List.of(caseType1, caseType2));
        return jurisdictionDefinition;
    }


    @Test
    public void testJurisdictionListsAreCached() {
        verify(caseDefinitionRepository, times(0)).getJurisdiction(JURISDICTION_ID_1);
        cachedCaseDefinitionRepository.getJurisdiction(JURISDICTION_ID_1);
        verify(caseDefinitionRepository, times(1)).getJurisdiction(JURISDICTION_ID_1);
        cachedCaseDefinitionRepository.getJurisdiction(JURISDICTION_ID_1);
        verify(caseDefinitionRepository, times(1)).getJurisdiction(JURISDICTION_ID_1);
        cachedCaseDefinitionRepository.getJurisdiction(JURISDICTION_ID_1);
        verify(caseDefinitionRepository, times(1)).getJurisdiction(JURISDICTION_ID_1);
    }

    @Disabled
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void testJurisdictionDefinitionPreventCacheManipulation() {
        doReturn(List.of(createJurisdictionDefinition(JURISDICTION_ID_1))).when(this.caseDefinitionRepository)
            .retrieveJurisdictions(Optional.of(List.of(JURISDICTION_ID_4)));

        var jurisdictionDefinitionFirstAttempt = caseDefinitionRepository.getJurisdiction(JURISDICTION_ID_4);
        jurisdictionDefinitionFirstAttempt.setCaseTypeDefinitions(null);
        assertNull(jurisdictionDefinitionFirstAttempt.getCaseTypeDefinitions());

        var jurisdictionDefinitionSecondAttempt = caseDefinitionRepository.getJurisdiction(JURISDICTION_ID_4);

        assertAll(
            () -> assertNotNull(jurisdictionDefinitionSecondAttempt.getCaseTypeDefinitions()),
            () -> assertNotEquals(jurisdictionDefinitionFirstAttempt.hashCode(),
                jurisdictionDefinitionSecondAttempt.hashCode())
        );

        verify(caseDefinitionRepository, times(1)).getJurisdiction(JURISDICTION_ID_4);
    }

    @Disabled
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void testJurisdictionDefinitionListPreventCacheManipulation() {
        var expectedJurisdictionDefinitionList = Arrays.asList(createJurisdictionDefinition(JURISDICTION_ID_1),
            createJurisdictionDefinition(JURISDICTION_ID_2));
        doReturn(expectedJurisdictionDefinitionList).when(this.caseDefinitionRepository)
            .retrieveJurisdictions(Optional.of(Collections.emptyList()));

        var jurisdictionDefinitionListFirstAttempt = caseDefinitionRepository.getAllJurisdictionsFromDefinitionStore();
        assertNotNull(jurisdictionDefinitionListFirstAttempt.get(0).getCaseTypeDefinitions());
        jurisdictionDefinitionListFirstAttempt.get(0).setCaseTypeDefinitions(null);
        assertNull(jurisdictionDefinitionListFirstAttempt.get(0).getCaseTypeDefinitions());

        var jurisdictionDefinitionListSecondAttempt = caseDefinitionRepository.getAllJurisdictionsFromDefinitionStore();

        assertAll(
            () -> assertNotNull(jurisdictionDefinitionListSecondAttempt.get(0).getCaseTypeDefinitions()),
            () -> assertNotEquals(jurisdictionDefinitionListFirstAttempt.stream().map(Object::hashCode).toList(),
                jurisdictionDefinitionListSecondAttempt.stream().map(Object::hashCode).toList()),
            () -> assertEquals(expectedJurisdictionDefinitionList.stream().map(JurisdictionDefinition::getId).toList(),
                jurisdictionDefinitionListSecondAttempt.stream().map(JurisdictionDefinition::getId).toList()),
            () -> assertNotEquals(expectedJurisdictionDefinitionList.stream().map(Object::hashCode).toList(),
                jurisdictionDefinitionListSecondAttempt.stream().map(Object::hashCode).toList())
        );

        verify(caseDefinitionRepository, times(1)).getAllJurisdictionsFromDefinitionStore();
    }

    @Test
    public void testTtlBasedEvictionOfJurisdictionLists() throws InterruptedException {
        Assert.assertEquals(3, applicationParams.getJurisdictionTTLSecs());

        verify(caseDefinitionRepository, times(0)).retrieveJurisdictions(Optional.of(List.of(JURISDICTION_ID_2)));
        caseDefinitionRepository.getJurisdiction(JURISDICTION_ID_2);
        verify(caseDefinitionRepository, times(1)).retrieveJurisdictions(Optional.of(List.of(JURISDICTION_ID_2)));

        caseDefinitionRepository.getJurisdiction(JURISDICTION_ID_2);
        caseDefinitionRepository.getJurisdiction(JURISDICTION_ID_2);
        caseDefinitionRepository.getJurisdiction(JURISDICTION_ID_2);
        verify(caseDefinitionRepository, times(1)).retrieveJurisdictions(Optional.of(List.of(JURISDICTION_ID_2)));

        TimeUnit.SECONDS.sleep(1);
        caseDefinitionRepository.getJurisdiction(JURISDICTION_ID_2);
        caseDefinitionRepository.getJurisdiction(JURISDICTION_ID_2);
        caseDefinitionRepository.getJurisdiction(JURISDICTION_ID_2);
        verify(caseDefinitionRepository, times(1)).retrieveJurisdictions(Optional.of(List.of(JURISDICTION_ID_2)));

        TimeUnit.SECONDS.sleep(2);
        verify(caseDefinitionRepository, times(1)).retrieveJurisdictions(Optional.of(List.of(JURISDICTION_ID_2)));
        caseDefinitionRepository.getJurisdiction(JURISDICTION_ID_2);
        verify(caseDefinitionRepository, times(2)).retrieveJurisdictions(Optional.of(List.of(JURISDICTION_ID_2)));

        caseDefinitionRepository.getJurisdiction(JURISDICTION_ID_2);
        caseDefinitionRepository.getJurisdiction(JURISDICTION_ID_2);
        caseDefinitionRepository.getJurisdiction(JURISDICTION_ID_2);
        verify(caseDefinitionRepository, times(2)).retrieveJurisdictions(Optional.of(List.of(JURISDICTION_ID_2)));
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

        verify(httpUIDefinitionGateway, times(1)).getSearchInputFieldDefinitions(VERSION_1, ID_1);
    }

    @Test
    public void testWizardPageDefinitionsAreCached() {
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
