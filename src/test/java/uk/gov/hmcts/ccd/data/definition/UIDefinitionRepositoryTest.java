package uk.gov.hmcts.ccd.data.definition;

import org.junit.Ignore;
import org.junit.Test;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import org.junit.jupiter.api.DisplayName;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResult;

import uk.gov.hmcts.ccd.domain.model.definition.WorkbasketInputDefinition;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import javax.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Ignore
public class UIDefinitionRepositoryTest extends WireMockBaseTest {

    @Inject
    private UIDefinitionRepository uiDefinitionRepository;

    @Test
    public void getDefinition() {
        final SearchResult workBasketResult = uiDefinitionRepository.getWorkBasketResult("TestAddressBookCase");
        assertThat(workBasketResult.getFields().length, is(3));
    }

    @Test
    @DisplayName("should Return Workbasket Input Definitions")
    public void shouldReturnWorkbasketInputDefinitions() {
        final WorkbasketInputDefinition workbasketInputDefinitions = uiDefinitionRepository
            .getWorkbasketInputDefinitions("TestAddressBookCase");
        assertThat(workbasketInputDefinitions.getFields().size(), is(3));
    }

    @Test(expected = ServiceException.class)
    public void shouldGetServiceExceptionWhenGettingWorkbasketInputDefinitions() {
            wireMockRule.stop();
            uiDefinitionRepository.getWorkbasketInputDefinitions("TestAddressBookCase");
    }

    @Test(expected = ServiceException.class)
    public void shouldGetServiceExceptionWhenGettingWorkbasketResults() {
            wireMockRule.stop();
            uiDefinitionRepository.getWorkBasketResult("TestAddressBookCase");
    }

    @Test(expected = ServiceException.class)
    public void shouldGetServiceExceptionWhenGettingSearchInputs() {
        wireMockRule.stop();
        uiDefinitionRepository.getWorkbasketInputDefinitions("TestAddressBookCase");
    }

    @Test(expected = ServiceException.class)
    public void shouldGetServiceExceptionWhenGettingSearchResults() {
        wireMockRule.stop();
        uiDefinitionRepository.getSearchResult("TestAddressBookCase");
    }
}
