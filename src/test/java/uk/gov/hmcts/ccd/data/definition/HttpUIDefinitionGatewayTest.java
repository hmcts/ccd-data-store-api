package uk.gov.hmcts.ccd.data.definition;

import com.google.common.collect.Lists;
import java.util.List;
import javax.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResultDefinition;

public class HttpUIDefinitionGatewayTest extends WireMockBaseTest {

    private static final int VERSION = 33;
    private static final String ORG_CASES = "ORGCASES";

    @Inject
    private HttpUIDefinitionGateway httpUIDefinitionGateway;

    @Test
    public void getDefinition() {
        final SearchResultDefinition workBasketResult = httpUIDefinitionGateway.getWorkBasketResult(VERSION,"TestAddressBookCase");
        assertThat(workBasketResult.getFields().length, is(3));
    }

    @Test
    @DisplayName("should Return Workbasket Input Definitions")
    public void shouldReturnWorkbasketInputDefinitions() {
        final WorkbasketInputFieldsDefinition workbasketInputFieldsDefinitions = httpUIDefinitionGateway
            .getWorkbasketInputFieldsDefinitions(VERSION, "TestAddressBookCase");
        assertThat(workbasketInputFieldsDefinitions.getFields().size(), is(3));
    }

    @Test
    @DisplayName("should Return banners")
    public void shouldReturnBanners() {
        List<String> jurisdictionIds = Lists.newArrayList("PROBATE", "DIVORCE");
        final BannersResult bannersResult = httpUIDefinitionGateway.getBanners(jurisdictionIds);
        assertThat(bannersResult.getBanners().size(), is(2));
    }

    @Test
    @DisplayName("should Return jurisdiction UI configs")
    public void shouldReturnJurisdictionUiConfigs() {
        List<String> jurisdictionIds = Lists.newArrayList("PROBATE", "DIVORCE");
        final JurisdictionUiConfigResult configResult = httpUIDefinitionGateway.getJurisdictionUiConfigs(jurisdictionIds);
        assertThat(configResult.getConfigs().size(), is(2));
    }

    @Test
    @DisplayName("should Return search cases result fields")
    public void shouldReturnSearchCasesResultFields() {
        final SearchResultDefinition searchResult = httpUIDefinitionGateway.getSearchCasesResultDefinition(VERSION, "TestAddressBookCase", ORG_CASES);
        assertAll(
            () -> assertThat(searchResult.getFields().length, is(7)),
            () -> assertThat(searchResult.getFields()[0].getUseCase(), is(ORG_CASES))
        );
    }
}
