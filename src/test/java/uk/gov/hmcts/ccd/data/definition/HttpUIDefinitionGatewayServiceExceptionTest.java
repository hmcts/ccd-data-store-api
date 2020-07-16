package uk.gov.hmcts.ccd.data.definition;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import javax.inject.Inject;
import java.util.List;

public class HttpUIDefinitionGatewayServiceExceptionTest extends WireMockBaseTest {

    private static final int VERSION = 33;

    @Inject
    private HttpUIDefinitionGateway httpUIDefinitionGateway;


    @Before
    public void setUp() {
        ReflectionTestUtils.setField(applicationParams, "uiDefinitionHost", "http://localhost:6666");
    }

    @Test(expected = ServiceException.class)
    public void shouldGetServiceExceptionWhenGettingWorkbasketInputDefinitions() {
        httpUIDefinitionGateway.getWorkbasketInputFieldsDefinitions(VERSION, "TestAddressBookCase");
    }

    @Test(expected = ServiceException.class)
    public void shouldGetServiceExceptionWhenGettingWorkbasketResults() {
        httpUIDefinitionGateway.getWorkBasketResult(VERSION, "TestAddressBookCase");
    }

    @Test(expected = ServiceException.class)
    public void shouldGetServiceExceptionWhenGettingSearchInputs() {
        httpUIDefinitionGateway.getWorkbasketInputFieldsDefinitions(VERSION, "TestAddressBookCase");
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
