package uk.gov.hmcts.ccd.data.definition;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResult;
import uk.gov.hmcts.ccd.domain.model.definition.WorkbasketInputDefinition;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

public class HttpUIDefinitionGatewayTest extends WireMockBaseTest {

    private static final int VERSION = 33;

    @Inject
    private HttpUIDefinitionGateway httpUIDefinitionGateway;

    @Test
    public void getDefinition() {
        final SearchResult workBasketResult = httpUIDefinitionGateway.getWorkBasketResult(VERSION,"TestAddressBookCase");
        assertThat(workBasketResult.getFields().length, is(3));
    }

    @Test
    @DisplayName("should Return Workbasket Input Definitions")
    public void shouldReturnWorkbasketInputDefinitions() {
        final WorkbasketInputDefinition workbasketInputDefinitions = httpUIDefinitionGateway
            .getWorkbasketInputDefinitions(VERSION, "TestAddressBookCase");
        assertThat(workbasketInputDefinitions.getFields().size(), is(3));
    }

    @Test(expected = ServiceException.class)
    public void shouldGetServiceExceptionWhenGettingWorkbasketInputDefinitions() {
            wireMockRule.stop();
            httpUIDefinitionGateway.getWorkbasketInputDefinitions(VERSION, "TestAddressBookCase");
    }

    @Test(expected = ServiceException.class)
    public void shouldGetServiceExceptionWhenGettingWorkbasketResults() {
            wireMockRule.stop();
            httpUIDefinitionGateway.getWorkBasketResult(VERSION, "TestAddressBookCase");
    }

    @Test(expected = ServiceException.class)
    public void shouldGetServiceExceptionWhenGettingSearchInputs() {
        wireMockRule.stop();
        httpUIDefinitionGateway.getWorkbasketInputDefinitions(VERSION, "TestAddressBookCase");
    }

    @Test(expected = ServiceException.class)
    public void shouldGetServiceExceptionWhenGettingSearchResults() {
        wireMockRule.stop();
        httpUIDefinitionGateway.getSearchResult(VERSION, "TestAddressBookCase");
    }
}
