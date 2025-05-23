package uk.gov.hmcts.ccd.data.definition;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import jakarta.inject.Inject;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HttpUIDefinitionGatewayServiceExceptionTest extends WireMockBaseTest {

    private static final int VERSION = 33;

    @Inject
    private HttpUIDefinitionGateway httpUIDefinitionGateway;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(applicationParams, "uiDefinitionHost", "http://localhost:6666");
    }

    @Test
    public void shouldGetServiceExceptionWhenGettingWorkbasketInputDefinitions() {
        assertThrows(ServiceException.class, () -> {
            httpUIDefinitionGateway.getWorkbasketInputFieldsDefinitions(VERSION, "TestAddressBookCase");
        });
    }

    @Test
    public void shouldGetServiceExceptionWhenGettingWorkbasketResults() {
        assertThrows(ServiceException.class, () -> {
            httpUIDefinitionGateway.getWorkBasketResult(VERSION, "TestAddressBookCase");
        });
    }

    @Test
    public void shouldGetServiceExceptionWhenGettingSearchInputs() {
        assertThrows(ServiceException.class, () -> {
            httpUIDefinitionGateway.getWorkbasketInputFieldsDefinitions(VERSION, "TestAddressBookCase");
        });
    }

    @Test
    public void shouldGetServiceExceptionWhenGettingSearchResults() {
        assertThrows(ServiceException.class, () -> {
            httpUIDefinitionGateway.getSearchResult(VERSION, "TestAddressBookCase");
        });
    }

    @Test
    public void shouldGetServiceExceptionWhenGettinBanners() {
        List<String> jurisdictionIds = Lists.newArrayList("PROBATE", "DIVORCE");
        assertThrows(ServiceException.class, () -> {
            httpUIDefinitionGateway.getBanners(jurisdictionIds);
        });
    }
}
