package uk.gov.hmcts.ccd.integrations;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseTypeDefinitionVersion;
import uk.gov.hmcts.ccd.data.definition.DefaultCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.HttpUIDefinitionGateway;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTabCollection;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.SearchInputDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResult;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.model.definition.WorkbasketInputDefinition;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DefinitionsCachingIT {

    private static final String ID = "case1";
    private static final String EVENT_ID = "event1";
    private static final int VERSION = 33;

    @SpyBean
    private DefaultCaseDefinitionRepository caseDefinitionRepository;

    @Autowired
    CachedCaseDefinitionRepository cachedCaseDefinitionRepository;

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

    List<WizardPage> wizardPageList = Collections.emptyList();

    @Before
    public void setup() {

        doReturn(aCaseTypeDefVersion(VERSION)).when(this.caseDefinitionRepository).getLatestVersion(ID);
        doReturn(mockCaseType).when(this.caseDefinitionRepository).getCaseType(ID);
    }

    @Test
    public void testCaseDefinitionAreCached() {

        cachedCaseDefinitionRepository.getCaseType(ID);
        cachedCaseDefinitionRepository.getCaseType(ID);
        cachedCaseDefinitionRepository.getCaseType(ID);

        verify(caseDefinitionRepository, times(1)).getCaseType(VERSION, ID);
        verify(caseDefinitionRepository, times(1)).getCaseType(ID);
    }

    @Test
    public void testWorkbasketInputDefinitionsAreCached() {

        doReturn(workbasketInputDefinition).when(this.httpUIDefinitionGateway).getWorkbasketInputDefinitions(VERSION, ID);

        uiDefinitionRepository.getWorkbasketInputDefinitions(ID);
        uiDefinitionRepository.getWorkbasketInputDefinitions(ID);
        uiDefinitionRepository.getWorkbasketInputDefinitions(ID);

        verify(httpUIDefinitionGateway, times(1)).getWorkbasketInputDefinitions(VERSION, ID);
    }

    @Test
    public void testWorkbasketResultAreCached() {

        doReturn(searchResult).when(this.httpUIDefinitionGateway).getWorkBasketResult(VERSION, ID);

        uiDefinitionRepository.getWorkBasketResult(ID);
        uiDefinitionRepository.getWorkBasketResult(ID);
        uiDefinitionRepository.getWorkBasketResult(ID);

        verify(httpUIDefinitionGateway, times(1)).getWorkBasketResult(VERSION, ID);
    }

    @Test
    public void testSearchResultAreCached() {

        doReturn(searchResult).when(this.httpUIDefinitionGateway).getSearchResult(VERSION, ID);

        uiDefinitionRepository.getSearchResult(ID);
        uiDefinitionRepository.getSearchResult(ID);
        uiDefinitionRepository.getSearchResult(ID);

        verify(httpUIDefinitionGateway, times(1)).getSearchResult(VERSION, ID);
    }

    @Test
    public void testCaseTabsAreCached() {

        doReturn(caseTabCollection).when(this.httpUIDefinitionGateway).getCaseTabCollection(VERSION, ID);

        uiDefinitionRepository.getCaseTabCollection(ID);
        uiDefinitionRepository.getCaseTabCollection(ID);
        uiDefinitionRepository.getCaseTabCollection(ID);

        verify(httpUIDefinitionGateway, times(1)).getCaseTabCollection(VERSION, ID);
    }

    @Test
    public void testSearchInputDefinitionsAreCached() {

        doReturn(searchInputDefinition).when(this.httpUIDefinitionGateway).getSearchInputDefinitions(VERSION, ID);

        uiDefinitionRepository.getSearchInputDefinitions(ID);
        uiDefinitionRepository.getSearchInputDefinitions(ID);
        uiDefinitionRepository.getSearchInputDefinitions(ID);

        verify(httpUIDefinitionGateway, times(1)).getSearchInputDefinitions(VERSION, ID);
    }

    @Test
    public void testWizardPageDefinitionsAreCached() {

        doReturn(wizardPageList).when(this.httpUIDefinitionGateway).getWizardPageCollection(VERSION, ID, EVENT_ID);

        uiDefinitionRepository.getWizardPageCollection(ID, EVENT_ID);
        uiDefinitionRepository.getWizardPageCollection(ID, EVENT_ID);
        uiDefinitionRepository.getWizardPageCollection(ID, EVENT_ID);

        verify(httpUIDefinitionGateway, times(1)).getWizardPageCollection(VERSION, ID, EVENT_ID);
    }

    protected CaseTypeDefinitionVersion aCaseTypeDefVersion(int version) {
        CaseTypeDefinitionVersion ctdv = new CaseTypeDefinitionVersion();
        ctdv.setVersion(version);
        return ctdv;
    }
}


