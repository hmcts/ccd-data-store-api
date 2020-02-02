package uk.gov.hmcts.ccd.data.definition;

import com.google.common.collect.Lists;
import java.util.List;
import javax.inject.Inject;

import org.junit.Test;
import uk.gov.hmcts.ccd.BaseTest;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

public class HttpUIDefinitionGatewayServiceExceptionTest extends BaseTest {

    private static final int VERSION = 33;

    @Inject
    private HttpUIDefinitionGateway httpUIDefinitionGateway;

    @Test(expected = ServiceException.class)
    public void shouldGetServiceExceptionWhenGettingWorkbasketInputDefinitions() {
            httpUIDefinitionGateway.getWorkbasketInputDefinitions(VERSION, "TestAddressBookCase");
    }

    @Test(expected = ServiceException.class)
    public void shouldGetServiceExceptionWhenGettingWorkbasketResults() {
            httpUIDefinitionGateway.getWorkBasketResult(VERSION, "TestAddressBookCase");
    }

    @Test(expected = ServiceException.class)
    public void shouldGetServiceExceptionWhenGettingSearchInputs() {
        httpUIDefinitionGateway.getWorkbasketInputDefinitions(VERSION, "TestAddressBookCase");
    }

    @Test(expected = ServiceException.class)
    public void shouldGetServiceExceptionWhenGettingSearchResults() {
        httpUIDefinitionGateway.getSearchResult(VERSION, "TestAddressBookCase");
    }

    @Test(expected = ServiceException.class)
    public void shouldGetServiceExceptionWhenGettinBanners() {
        List<String> jurisdictionIds = Lists.newArrayList("PROBATE", "DIVORCE");
        httpUIDefinitionGateway.getBanners(jurisdictionIds);
    }
}
